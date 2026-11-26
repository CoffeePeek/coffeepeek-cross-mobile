package com.coffeepeek.feature.roaster.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.roaster.impl.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeRoasterSessionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRoasterRepository()
    private val favorites = FakeFavorites()
    private val owners = mutableListOf<() -> Unit>()
    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() {
        owners.forEach { it() }
        store.clear()
        Dispatchers.resetMain()
    }
    private fun list(): NativeRoasterListSession {
        val model = RoasterListViewModel(repository, favorites)
        store.put("list", model)
        return NativeRoasterListSession(model)
    }
    private fun detail(): NativeRoasterDetailSession {
        val model = RoasterDetailViewModel("a", repository, favorites)
        store.put("detail", model)
        return NativeRoasterDetailSession(model)
    }

    @Test fun nativeCriteriaAndFavoritesUseTheSameFilteredListAndWriter() = runTest(dispatcher) {
        val session = list()
        favorites.keys.value = setOf("b")
        session.criteria("", setOf("b"), true)
        runCurrent()
        assertEquals(listOf("b"), session.current().cards.map { it.slug })
        assertTrue(session.current().cards.single().favorite)
        session.favorite("b")
        runCurrent()
        assertEquals("b", favorites.changes.single().first.key)
        assertFalse(favorites.changes.single().second)
    }

    @Test fun anExcludedOrUnknownCardCannotNavigateFromTheNativeList() = runTest(dispatcher) {
        val session = list()
        val opened = mutableListOf<String>()
        backgroundScope.launch { session.opened.collect(opened::add) }
        session.criteria("", setOf("b"), false)
        runCurrent()
        session.open("a")
        session.open("unknown")
        session.open("b")
        runCurrent()
        assertEquals(listOf("b"), opened)
    }

    @Test fun nativePhotoSelectionRemainsValidatedByTheSharedDetailModel() = runTest(dispatcher) {
        val session = detail()
        runCurrent()
        session.photo("private://not-in-this-roaster")
        runCurrent()
        assertNull(session.current().photo)
        val url = session.current().details!!.photos.single().url
        session.photo(url)
        runCurrent()
        assertEquals(url, session.current().photo)
        session.closePhoto()
        runCurrent()
        assertNull(session.current().photo)
    }

    @Test fun nativeFavoriteFailuresKeepTheExistingLocalizedErrorContract() = runTest(dispatcher) {
        val session = detail()
        favorites.failChange = true
        runCurrent()
        session.favorite()
        runCurrent()
        assertEquals("roaster_message_favorite_failed", session.current().messageKey)
        session.dismissMessage()
        runCurrent()
        assertNull(session.current().messageKey)
    }

    @Test fun standaloneListClosesItsModelAndRejectsLateNativeCommands() = runTest(dispatcher) {
        val session = createNativeRoasterListSession(repository, favorites).also { owners.add(it::close) }
        runCurrent()
        val reads = repository.catalogCalls
        session.close()
        session.close()
        session.retry()
        session.favorite("a")
        runCurrent()
        assertEquals(reads, repository.catalogCalls)
        assertTrue(favorites.changes.isEmpty())
        assertTrue(session.current().cards.isEmpty())
    }

    @Test fun standaloneDetailClosesItsModelAndRejectsLateNativeCommands() = runTest(dispatcher) {
        val session = createNativeRoasterDetailSession("a", repository, favorites).also { owners.add(it::close) }
        runCurrent()
        val reads = repository.detailCalls.size
        session.close()
        session.close()
        session.retry()
        session.favorite()
        session.photo("https://fixture.invalid/photo")
        runCurrent()
        assertEquals(reads, repository.detailCalls.size)
        assertTrue(favorites.changes.isEmpty())
        assertNull(session.current().details)
    }
}
