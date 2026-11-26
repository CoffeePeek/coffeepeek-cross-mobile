package com.coffeepeek.feature.collections.impl

import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollectionCover
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.SavedTestAccount
import com.coffeepeek.feature.collections.impl.ui.SavedTestRepository
import com.coffeepeek.feature.collections.impl.ui.SavedTestSnapshots
import com.coffeepeek.feature.collections.impl.ui.privateList
import com.coffeepeek.feature.collections.impl.ui.publicList
import com.coffeepeek.feature.collections.impl.ui.savedShop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsPublicWorkspaceSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = SavedTestRepository()
    private val account = SavedTestAccount().apply { owner.value = null }
    private val events = mutableListOf<String>()
    private val sessions = mutableListOf<CollectionsWorkspaceSession>()

    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)

    @AfterTest fun after() {
        sessions.forEach(CollectionsWorkspaceSession::close)
        Dispatchers.resetMain()
    }

    @Test fun guestsOpenPlacesAndShare() = runTest(dispatcher) {
        val roaster = savedShop.copy(kind = CollectionPlaceKind.Roaster, slug = "roaster")
        val repository = object : SavedCollectionsRepository by saved {
            override suspend fun public(id: String) = Result.success(publicList.copy(places = listOf(savedShop, roaster)))
        }
        val session = session(repository)
        runCurrent()
        listOf(savedShop.key, roaster.key).forEach { key ->
            session.openPlace(key)
            runCurrent()
        }
        session.shareLink()
        runCurrent()
        assertEquals(listOf("shop:shop", "roaster:roaster", "Public\nhttps://fixture.invalid/collections/public"), events)
        session.back()
        runCurrent()
        assertEquals("back", events.last())
        assertEquals(0, account.loads)
    }

    @Test fun posterBackKeepsTheListOpen() = runTest(dispatcher) {
        val session = session(saved)
        runCurrent()
        session.preview()
        runCurrent()
        assertTrue(session.current().preview)
        session.back()
        runCurrent()
        assertFalse(session.current().preview)
        assertTrue(events.isEmpty())
    }

    @Test fun publicDefaultCoverUsesLoadedPlacesWithoutAnotherDetailRead() = runTest(dispatcher) {
        val photo = "https://fixture.invalid/shop.jpg"
        var reads = 0
        val repository = object : SavedCollectionsRepository by saved {
            override suspend fun public(id: String) = Result.success(publicList.copy(
                places = listOf(savedShop.copy(photoUrl = photo)),
            )).also { reads++ }
        }
        val session = session(repository)
        runCurrent()
        var url: String? = null
        session.loadCover("public", session.current().accountVersion) { bytes, value ->
            assertNull(bytes)
            url = value
        }
        runCurrent()
        assertEquals(photo, url)
        assertEquals(1, reads)
    }

    @Test fun publicCoverNeverReadsAuthenticatedBytes() = runTest(dispatcher) {
        account.owner.value = "signed-in-viewer"
        val repository = object : SavedCollectionsRepository by saved {
            override suspend fun public(id: String) = Result.success(publicList.copy(
                summary = publicList.summary.copy(cover = SavedCollectionCover("owned", authenticated = true)),
            ))
            override suspend fun cover(collectionId: String, cover: SavedCollectionCover): Result<ByteArray> =
                error("Authenticated cover must not be requested by public viewer")
        }
        val public = session(repository)
        runCurrent()
        var coverDelivered = false
        public.loadCover("public", public.current().accountVersion) { bytes, url ->
            assertNull(bytes)
            assertNull(url)
            coverDelivered = true
        }
        runCurrent()
        assertTrue(coverDelivered)
    }

    @Test fun privateCollectionExposesNoActions() = runTest(dispatcher) {
        val privateSession = session(object : SavedCollectionsRepository by saved {
            override suspend fun public(id: String) = Result.success(privateList)
        })
        runCurrent()
        privateSession.shareLink()
        privateSession.openPlace(savedShop.key)
        privateSession.preview()
        runCurrent()
        assertEquals("", privateSession.current().title)
        assertFalse(privateSession.current().preview)
        assertTrue(events.isEmpty())
    }

    private fun session(repository: SavedCollectionsRepository): CollectionsWorkspaceSession =
        CollectionsWorkspaceSession(repository, SavedTestSnapshots(), account, CollectionsWorkspaceMode.Public,
            "public", null, null, { events.add("back") }, {}, {}, {},
            { events.add("shop:$it") }, { events.add("roaster:$it") }, events::add, onSaved = { events.add("saved") }).also(sessions::add)
}
