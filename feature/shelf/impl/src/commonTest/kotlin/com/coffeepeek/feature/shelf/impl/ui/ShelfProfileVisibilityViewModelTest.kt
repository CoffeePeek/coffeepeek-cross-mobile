package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfProfileVisibilityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: FakeShelfProfileRepository) = ShelfProfileVisibilityViewModel(repository).also { store.put("profile", it) }

    @Test fun aGuestNeverReadsOrWritesOwnerPreferencesAndCanRequestSignIn() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository(null)
        val model = model(repository)
        val event = async { model.events.first() }
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        model.onAction(ShelfProfileVisibilityAction.SignIn)
        runCurrent()
        assertEquals(ShelfProfileVisibilityEvent.SignIn, event.await())
        assertFalse(model.state.value.signedIn)
        assertFalse(model.state.value.visible)
        assertEquals(0, repository.visibilityReads)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun aFailedLoadDoesNotAllowAccidentalPublishing() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.failure(IllegalStateException()) } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertFalse(model.state.value.loaded)
        assertEquals(ShelfProfileVisibilityError.Load, model.state.value.error)
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun publishingWaitsForThePersistedResponseAndRejectsDuplicateWork() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityWrite = { gate.await() } }
        val model = model(repository)
        runCurrent()
        repeat(2) { model.onAction(ShelfProfileVisibilityAction.SetVisible(true)) }
        runCurrent()
        assertTrue(model.state.value.busy)
        assertFalse(model.state.value.visible)
        assertEquals(listOf(true), repository.writes)
        gate.complete(Result.success(true))
        runCurrent()
        assertTrue(model.state.value.visible)
        assertFalse(model.state.value.busy)
    }

    @Test fun aSaveFailureKeepsTheServerConfirmedValue() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityWrite = { Result.failure(IllegalStateException()) } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertTrue(model.state.value.loaded)
        assertEquals(ShelfProfileVisibilityError.Save, model.state.value.error)
        repository.visibilityWrite = { Result.success(it) }
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertTrue(model.state.value.visible)
        assertEquals(listOf(true, true), repository.writes)
    }

    @Test fun aDifferentPersistedValueWinsOverTheRequestedValue() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityWrite = { Result.success(false) } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertNull(model.state.value.error)
    }

    @Test fun aRefreshPreservesTheConfirmedPreferenceWhenTheRequestFails() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.success(true) } }
        val model = model(repository)
        runCurrent()
        repository.visibilityRead = { gate.await() }
        model.onAction(ShelfProfileVisibilityAction.Refresh)
        runCurrent()
        assertTrue(model.state.value.visible)
        assertTrue(model.state.value.loaded)
        assertFalse(model.state.value.loading)
        gate.complete(Result.failure(IllegalStateException()))
        runCurrent()
        assertTrue(model.state.value.visible)
        assertEquals(ShelfProfileVisibilityError.Load, model.state.value.error)
    }

    @Test fun aNewScreenStartsWithConfirmedTrueAndRefreshesWithoutHidingIt() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.success(true) } }
        model(repository)
        runCurrent()
        repository.visibilityRead = { gate.await() }
        val reopened = model(repository)
        assertTrue(reopened.state.value.visible)
        assertTrue(reopened.state.value.loaded)
        runCurrent()
        assertFalse(reopened.state.value.loading)
        gate.complete(Result.failure(IllegalStateException()))
        runCurrent()
        assertTrue(reopened.state.value.visible)
    }

    @Test fun confirmedFalseIsARealCachedPreferenceOnReentry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.success(false) } }
        model(repository)
        runCurrent()
        repository.visibilityRead = { gate.await() }
        val reopened = model(repository)
        assertFalse(reopened.state.value.visible)
        assertTrue(reopened.state.value.loaded)
        runCurrent()
        assertFalse(reopened.state.value.loading)
        gate.complete(Result.success(false))
        runCurrent()
    }

    @Test fun aTapDuringBackgroundRefreshKeepsTheLastConfirmedWrite() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository()
        val model = model(repository)
        runCurrent()
        repository.visibilityRead = { withContext(NonCancellable) { gate.await() } }
        model.onAction(ShelfProfileVisibilityAction.Refresh)
        runCurrent()
        assertFalse(model.state.value.loading)
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertTrue(model.state.value.visible)
        gate.complete(Result.success(false))
        runCurrent()
        assertTrue(model.state.value.visible)
    }

    @Test fun aNewLoginForTheSameUserCannotReuseTheOldVisibility() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.success(true) } }
        val model = model(repository)
        runCurrent()
        repository.visibilityRead = { Result.success(false) }
        repository.sessionGeneration.value++
        runCurrent()
        assertFalse(model.state.value.visible)
        assertTrue(model.state.value.loaded)
        assertEquals(2, repository.visibilityReads)
    }

    @Test fun aLatePreviousAccountReadCannotExposeItsPreference() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { withContext(NonCancellable) { gate.await() } } }
        val model = model(repository)
        runCurrent()
        repository.visibilityRead = { Result.success(false) }
        repository.owners.value = "next-owner"
        runCurrent()
        gate.complete(Result.success(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertTrue(model.state.value.loaded)
    }

    @Test fun signingOutDiscardsAnInFlightPublishingResult() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Boolean>>()
        val repository = FakeShelfProfileRepository().apply { visibilityWrite = { withContext(NonCancellable) { gate.await() } } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        repository.owners.value = null
        runCurrent()
        gate.complete(Result.success(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertFalse(model.state.value.signedIn)
        assertFalse(model.state.value.loaded)
        assertFalse(model.state.value.busy)
    }

    @Test fun cancellationIsAnErrorForTheSameAccountInsteadOfAConfirmedHiddenPreference() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityRead = { Result.failure(CancellationException()) } }
        val model = model(repository)
        runCurrent()
        assertFalse(model.state.value.loaded)
        assertFalse(model.state.value.loading)
        assertEquals(ShelfProfileVisibilityError.Load, model.state.value.error)
    }

    @Test fun aCancelledWriteRetainsTheLastConfirmedPreference() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { visibilityWrite = { Result.failure(CancellationException()) } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfProfileVisibilityAction.SetVisible(true))
        runCurrent()
        assertFalse(model.state.value.visible)
        assertFalse(model.state.value.busy)
        assertEquals(ShelfProfileVisibilityError.Save, model.state.value.error)
    }
}
