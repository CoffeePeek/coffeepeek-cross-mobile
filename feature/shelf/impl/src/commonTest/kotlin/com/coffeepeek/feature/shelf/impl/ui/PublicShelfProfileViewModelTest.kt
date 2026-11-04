package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PublicShelfProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: FakeShelfProfileRepository) = PublicShelfProfileViewModel(repository, "author").also { store.put("public", it) }

    @Test fun aHiddenShelfRemainsAbsentWithoutReadingOwnerPreferencesOrPhotos() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository(null)
        val model = model(repository)
        runCurrent()
        assertNull(model.state.value.shelf)
        assertFalse(model.state.value.failed)
        assertFalse(model.state.value.loading)
        assertEquals(listOf("author"), repository.publicSlugs)
        assertEquals(0, repository.visibilityReads)
        assertEquals(0, repository.photoCalls)
    }

    @Test fun onlyPublishedCatalogueSlugsProduceNavigationEvents() = runTest(dispatcher) {
        val published = profileShelf().items.single()
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(PublicShelf(
            listOf(published, published.copy(id = "blank", coffeeSlug = " "), published.copy(id = "manual", coffeeSlug = null)), 3)) } }
        val model = model(repository)
        val events = mutableListOf<PublicShelfProfileEvent>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        listOf("bag", "blank", "manual", "missing").forEach { model.onAction(PublicShelfProfileAction.OpenCoffee(it)) }
        runCurrent()
        assertEquals(listOf<PublicShelfProfileEvent>(PublicShelfProfileEvent.OpenCoffee("ethiopia")), events)
    }

    @Test fun refreshImmediatelyDropsTheOldPublicProjectionAndRejectsStaleClicks() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<PublicShelf?>>()
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(profileShelf()) } }
        val model = model(repository)
        val events = mutableListOf<PublicShelfProfileEvent>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        assertNotNull(model.state.value.shelf)
        repository.publicRead = { gate.await() }
        model.onAction(PublicShelfProfileAction.Refresh)
        model.onAction(PublicShelfProfileAction.OpenCoffee("bag"))
        runCurrent()
        assertNull(model.state.value.shelf)
        assertTrue(model.state.value.loading)
        assertTrue(events.isEmpty())
        gate.complete(Result.success(null))
        runCurrent()
        assertNull(model.state.value.shelf)
    }

    @Test fun aRefreshFailureDoesNotRetainFormerlyPublicBags() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(profileShelf()) } }
        val model = model(repository)
        runCurrent()
        repository.publicRead = { Result.failure(IllegalStateException()) }
        model.onAction(PublicShelfProfileAction.Refresh)
        runCurrent()
        assertNull(model.state.value.shelf)
        assertTrue(model.state.value.failed)
    }

    @Test fun accountReplacementIgnoresALateOldProjection() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<PublicShelf?>>()
        val repository = FakeShelfProfileRepository().apply { publicRead = { withContext(NonCancellable) { gate.await() } } }
        val model = model(repository)
        runCurrent()
        repository.publicRead = { Result.success(null) }
        repository.owners.value = "next-owner"
        runCurrent()
        gate.complete(Result.success(profileShelf()))
        runCurrent()
        assertNull(model.state.value.shelf)
        assertFalse(model.state.value.loading)
    }

    @Test fun aCancelledPublicReadFailsClosed() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.failure(CancellationException()) } }
        val model = model(repository)
        runCurrent()
        assertNull(model.state.value.shelf)
        assertTrue(model.state.value.failed)
        assertFalse(model.state.value.loading)
    }
}
