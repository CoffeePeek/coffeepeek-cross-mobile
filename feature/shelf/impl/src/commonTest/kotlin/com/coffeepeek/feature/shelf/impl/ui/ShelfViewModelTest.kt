package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    private val arrivals = ShelfArrivals()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: FakeShelfRepository, handoff: ShelfBagHandoff = ShelfBagHandoff()) =
        ShelfViewModel(repository, testCalendar(), arrivals, handoff).also { store.put("shelf-${nextKey++}", it) }

    @Test fun showsTheRepositorySnapshotWithItsSummary() = runTest(dispatcher) {
        val repository = FakeShelfRepository()
        val model = model(repository)
        runCurrent()
        assertFalse(model.state.value.loaded)

        repository.shelfState.value = listOf(bag("a"), bag("b").copy(roasterName = "River", weightGrams = null))
        runCurrent()
        assertTrue(model.state.value.loaded)
        assertEquals(listOf("a", "b"), model.state.value.bags.map { it.id })
        assertEquals(ShelfSummary(bags = 2, grams = 250, roasters = 1), model.state.value.summary)
        assertEquals(7, model.state.value.bags[0].daysOnShelf(model.state.value.today))
    }

    @Test fun firstLoadFailureShowsAnErrorButALaterOneOnlyAMessage() = runTest(dispatcher) {
        val repository = FakeShelfRepository().apply { refreshResult = { Result.failure(ShelfFailure(ShelfIssue.Offline)) } }
        val model = model(repository)
        model.onAction(ShelfAction.Refresh)
        runCurrent()
        assertTrue(model.state.value.loadFailed)
        assertNull(model.state.value.message)

        repository.shelfState.value = listOf(bag("a"))
        model.onAction(ShelfAction.Refresh)
        runCurrent()
        assertEquals(ShelfMessage.Failed(ShelfIssue.Offline), model.state.value.message)
    }

    @Test fun guestRefreshIsNotAnError() = runTest(dispatcher) {
        val model = model(FakeShelfRepository().apply { refreshResult = { Result.failure(ShelfFailure(ShelfIssue.SignedOut)) } })
        model.onAction(ShelfAction.Refresh)
        runCurrent()
        assertFalse(model.state.value.loadFailed)
        assertNull(model.state.value.message)
    }

    @Test fun finishOffersUndoAtTheOldPositionAndUndoRestoresThere() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a"), bag("b"), bag("c")))
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfAction.StartFinish("b"))
        runCurrent()
        assertEquals("b", model.state.value.finishing?.id)

        model.onAction(ShelfAction.Finish("b", rating = 4, buyAgain = true))
        runCurrent()
        assertNull(model.state.value.finishingId)
        assertEquals(ShelfMessage.Finished("b", 1), model.state.value.message)
        assertEquals(listOf("a", "c"), model.state.value.bags.map { it.id })

        model.onAction(ShelfAction.UndoFinish("b", 1))
        runCurrent()
        assertNull(model.state.value.message)
        assertEquals(listOf("finish:b:4:true", "restore:b:1"), repository.calls)
        assertEquals(listOf("a", "b", "c"), model.state.value.bags.map { it.id })
    }

    @Test fun failedWritesReportTheirIssue() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a"), bag("b"))).apply { writeFailure = ShelfIssue.Stale }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfAction.Move("a", 1))
        runCurrent()
        assertEquals(ShelfMessage.Failed(ShelfIssue.Stale), model.state.value.message)
    }

    @Test fun moveToTheSamePlaceOrOfAnUnknownBagIsIgnored() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a"), bag("b")))
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfAction.Move("a", 0))
        model.onAction(ShelfAction.Move("x", 1))
        model.onAction(ShelfAction.Move("a", 1))
        runCurrent()
        assertEquals(listOf("move:a:1"), repository.calls)
    }

    @Test fun aSecondWriteToTheSameBagWaitsOut() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeShelfRepository(listOf(bag("a"), bag("b"))).apply { this.gate = { gate.await() } }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfAction.Finish("a", null, null))
        model.onAction(ShelfAction.UndoFinish("a", 0))
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("finish:a:null:null"), repository.calls)
    }

    @Test fun anArrivalIsMarkedAnnouncedAndThenCleared() = runTest(dispatcher) {
        val model = model(FakeShelfRepository(listOf(bag("new"), bag("a"))))
        runCurrent()
        arrivals.arrived("new")
        runCurrent()
        assertEquals("new", model.state.value.arrivingId)
        assertEquals(ShelfMessage.Added, model.state.value.message)
        model.onAction(ShelfAction.ArrivalShown("new"))
        runCurrent()
        assertNull(model.state.value.arrivingId)
        assertNull(arrivals.arriving.value)
    }

    @Test fun destinationsAreEvents() = runTest(dispatcher) {
        val model = model(FakeShelfRepository())
        model.onAction(ShelfAction.OpenArchive)
        runCurrent()
        assertEquals(ShelfEvent.OpenArchive, model.events.first())
    }

    @Test fun aTappedBagOpensOnItsScreenAndWhatBecameOfItThereIsReported() = runTest(dispatcher) {
        val handoff = ShelfBagHandoff()
        val model = model(FakeShelfRepository(listOf(bag("a"), bag("b"))), handoff)
        runCurrent()
        model.onAction(ShelfAction.OpenBag("a"))
        runCurrent()
        assertEquals(ShelfEvent.OpenBag("a"), model.events.first())
        assertEquals(bag("a"), handoff.opened("a"))

        handoff.report(ShelfBagOutcome.Finished("a", 0))
        runCurrent()
        assertEquals(ShelfMessage.Finished("a", 0), model.state.value.message)
        assertNull(handoff.outcome.value)

        handoff.report(ShelfBagOutcome.Deleted("b", archived = false))
        runCurrent()
        assertEquals(ShelfMessage.Deleted, model.state.value.message)

        // Restoring and deleting archived bags are the archive's to report.
        val restored = ShelfBagOutcome.Restored("x")
        handoff.report(restored)
        runCurrent()
        assertEquals(restored, handoff.outcome.value)
    }
}
