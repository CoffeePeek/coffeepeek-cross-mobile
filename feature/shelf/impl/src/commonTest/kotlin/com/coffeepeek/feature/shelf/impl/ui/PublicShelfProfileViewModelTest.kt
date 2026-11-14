package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.domain.repository.PublicShelfSnapshot
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

    @Test fun refreshKeepsTheProjectionUntilTheNewOneArrives() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<PublicShelf?>>()
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(profileShelf()) } }
        val model = model(repository)
        val events = mutableListOf<PublicShelfProfileEvent>()
        backgroundScope.launch { model.events.collect { events += it } }
        runCurrent()
        assertNotNull(model.state.value.shelf)
        repository.publicRead = { gate.await() }
        model.onAction(PublicShelfProfileAction.Refresh)
        runCurrent()
        // Stale-while-revalidate: the loaded projection stays visible during the refresh.
        assertNotNull(model.state.value.shelf)
        assertTrue(model.state.value.refreshing)
        gate.complete(Result.success(null))
        runCurrent()
        assertNull(model.state.value.shelf)
    }

    @Test fun aRefreshFailureKeepsTheProjectionAndMarksTheError() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(profileShelf()) } }
        val model = model(repository)
        runCurrent()
        repository.publicRead = { Result.failure(IllegalStateException()) }
        model.onAction(PublicShelfProfileAction.Refresh)
        runCurrent()
        // Stale-while-revalidate: the failed refresh does not blank the loaded shelf.
        assertNotNull(model.state.value.shelf)
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

    @Test fun ownShelfReopensFromCacheAndKeepsItDuringAFailedBackgroundRefresh() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply {
            cached = PublicShelfSnapshot(profileShelf())
            publicRead = { Result.failure(IllegalStateException()) }
        }
        val model = PublicShelfProfileViewModel(repository, "author", ownerView = true).also { store.put("own", it) }
        val revision = model.state.value.revision
        assertEquals(profileShelf(), model.state.value.shelf)
        assertFalse(model.state.value.loading)
        runCurrent()
        assertEquals(profileShelf(), model.state.value.shelf)
        assertEquals(revision, model.state.value.revision)
        assertFalse(model.state.value.loading)
        assertTrue(model.state.value.failed)
    }

    @Test fun aCachedHiddenOwnShelfDoesNotFlashAPlaceholder() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<PublicShelf?>>()
        val repository = FakeShelfProfileRepository().apply {
            cached = PublicShelfSnapshot(null)
            publicRead = { gate.await() }
        }
        val model = PublicShelfProfileViewModel(repository, "author", ownerView = true).also { store.put("own", it) }
        runCurrent()
        assertNull(model.state.value.shelf)
        assertFalse(model.state.value.loading)
        assertTrue(model.state.value.refreshing)
        gate.complete(Result.success(null))
        runCurrent()
        assertFalse(model.state.value.refreshing)
    }

    @Test fun ownCacheIsNeverUsedForAnotherAccountOrForPublicVisitors() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<PublicShelf?>>()
        val repository = FakeShelfProfileRepository().apply {
            cached = PublicShelfSnapshot(profileShelf())
            owners.value = "next-owner"
            publicRead = { gate.await() }
        }
        val own = PublicShelfProfileViewModel(repository, "author", ownerView = true).also { store.put("own", it) }
        assertNull(own.state.value.shelf)
        assertTrue(own.state.value.loading)
        repository.cachedOwner = "next-owner"
        val public = model(repository)
        assertNull(public.state.value.shelf)
        assertTrue(public.state.value.loading)
        gate.complete(Result.success(null))
        runCurrent()
    }
}
