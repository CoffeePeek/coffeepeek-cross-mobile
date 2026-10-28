package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.domain.model.ShelfBag
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagEvent
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
class ShelfBagViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    private val handoff = ShelfBagHandoff()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(id: String, repository: FakeShelfRepository, brews: FakeShelfBrewLog = FakeShelfBrewLog(), opened: ShelfBag? = null) =
        ShelfBagViewModel(id, repository, testCalendar(), handoff.apply { opened?.let(::open) }, brews)
            .also { store.put("bag-${nextKey++}", it) }

    @Test fun theHandedOverBagShowsAtOnceWithTheCheckInsOfItsDaysOnTheShelf() = runTest(dispatcher) {
        val brews = FakeShelfBrewLog(Result.success(listOf(
            brew("1", "2026-10-21", product = "p1", dose = 15.0, score = 4),
            brew("2", "2026-10-26", product = "p1", dose = 17.5),
            brew("3", "2026-10-25", product = "p2", dose = 18.0),
        )))
        val bag = bag("a").copy(coffeeProductId = "p1")
        val model = model("a", FakeShelfRepository(listOf(bag)), brews, opened = bag)
        assertEquals("a", model.state.value.bag?.id)
        runCurrent()
        assertEquals(listOf("2026-10-20" to "2026-10-27"), brews.ranges)
        val loaded = model.state.value.brews as ShelfBagBrews.Loaded
        assertEquals(listOf("2", "1"), loaded.brews.map { it.id })
        assertEquals(218, loaded.leftGrams)
    }

    @Test fun withoutAHandoverAShelfBagIsFoundOnTheShelfAndAnyOtherIsMissing() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a")))
        val found = model("a", repository)
        val gone = model("archived", repository)
        runCurrent()
        assertEquals("a", found.state.value.bag?.id)
        assertFalse(found.state.value.missing)
        assertNull(gone.state.value.bag)
        assertTrue(gone.state.value.missing)
    }

    @Test fun aBagWithoutACoffeeAsksForNoCheckIns() = runTest(dispatcher) {
        val brews = FakeShelfBrewLog()
        val model = model("a", FakeShelfRepository(listOf(bag("a").copy(name = " "))), brews)
        runCurrent()
        assertEquals(ShelfBagBrews.Unmatched, model.state.value.brews)
        assertTrue(brews.ranges.isEmpty())
    }

    @Test fun failedCheckInsCanBeRetried() = runTest(dispatcher) {
        val brews = FakeShelfBrewLog(Result.failure(IllegalStateException("offline")))
        val model = model("a", FakeShelfRepository(listOf(bag("a"))), brews)
        runCurrent()
        assertEquals(ShelfBagBrews.Failed, model.state.value.brews)

        brews.result = Result.success(listOf(brew("1", "2026-10-22", bean = "bag a")))
        model.onAction(ShelfBagAction.RetryBrews)
        runCurrent()
        assertEquals(listOf("1"), (model.state.value.brews as ShelfBagBrews.Loaded).brews.map { it.id })
    }

    @Test fun brewingOpensTheFormWithTheBagsCoffeeAndASavedCheckInIsListed() = runTest(dispatcher) {
        val brews = FakeShelfBrewLog()
        val model = model("a", FakeShelfRepository(listOf(bag("a").copy(coffeeProductId = "p1", coffeeProductSlug = "kenya"))), brews)
        runCurrent()
        model.onAction(ShelfBagAction.Brew)
        runCurrent()
        assertEquals(ShelfBrewCoffee("kenya", "Bag a"), model.state.value.brewing)

        brews.result = Result.success(listOf(brew("1", "2026-10-27", product = "p1")))
        model.onAction(ShelfBagAction.BrewClosed(saved = true))
        runCurrent()
        assertNull(model.state.value.brewing)
        assertEquals(listOf("1"), (model.state.value.brews as ShelfBagBrews.Loaded).brews.map { it.id })

        model.onAction(ShelfBagAction.OpenCoffee)
        runCurrent()
        assertEquals(ShelfBagEvent.OpenCoffee("kenya"), model.events.first())
    }

    @Test fun aFinishHandsBackItsShelfPositionAndCloses() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a"), bag("b"), bag("c")))
        val model = model("b", repository, opened = bag("b"))
        runCurrent()
        model.onAction(ShelfBagAction.StartFinish)
        runCurrent()
        assertTrue(model.state.value.finishing)

        model.onAction(ShelfBagAction.Finish(rating = 4, buyAgain = true))
        runCurrent()
        assertFalse(model.state.value.finishing)
        assertEquals(listOf("finish:b:4:true"), repository.calls)
        assertEquals(ShelfBagOutcome.Finished("b", 1), handoff.outcome.value)
        assertEquals(ShelfBagEvent.Back, model.events.first())
        // The bag left the shelf, but the screen keeps showing it until it is gone.
        assertEquals("b", model.state.value.bag?.id)
    }

    @Test fun anArchivedBagCanBePutBackOrDeleted() = runTest(dispatcher) {
        val repository = FakeShelfRepository(emptyList())
        val restoring = model("f", repository, opened = bag("f", finished = true))
        runCurrent()
        restoring.onAction(ShelfBagAction.StartFinish)
        restoring.onAction(ShelfBagAction.Restore)
        runCurrent()
        assertFalse(restoring.state.value.finishing)
        assertEquals(ShelfBagOutcome.Restored("f"), handoff.outcome.value)

        val deleting = model("g", repository, opened = bag("g", finished = true))
        runCurrent()
        deleting.onAction(ShelfBagAction.RequestDelete)
        runCurrent()
        assertTrue(deleting.state.value.deleting)
        deleting.onAction(ShelfBagAction.Delete)
        runCurrent()
        assertEquals(ShelfBagOutcome.Deleted("g", archived = true), handoff.outcome.value)
        assertEquals(listOf("restore:f:null", "delete:g"), repository.calls)
    }

    @Test fun aRefusedWriteKeepsTheScreenAndSaysWhy() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("a"))).apply { writeFailure = ShelfIssue.Stale }
        val model = model("a", repository)
        runCurrent()
        model.onAction(ShelfBagAction.Delete)
        runCurrent()
        assertEquals(ShelfIssue.Stale, model.state.value.failure)
        assertFalse(model.state.value.busy)
        assertNull(handoff.outcome.value)
    }

    @Test fun oneWriteAtATime() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeShelfRepository(listOf(bag("a"))).apply { this.gate = { gate.await() } }
        val model = model("a", repository, opened = bag("a"))
        runCurrent()
        model.onAction(ShelfBagAction.Finish(null, null))
        model.onAction(ShelfBagAction.Delete)
        runCurrent()
        assertTrue(model.state.value.busy)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("finish:a:null:null"), repository.calls)
    }
}
