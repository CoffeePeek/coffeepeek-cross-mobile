package com.coffeepeek.feature.coffee.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.impl.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: TestCoffeeRepository = TestCoffeeRepository()) =
        CoffeeListViewModel(repository).also { store.put("list-${nextKey++}", it) }

    @Test fun initializeReadsListAndLabelsIndependentlyAndOnlyOnce() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { filters = { gate.await(); Result.success(groups()) } }
        val model = model(repository)
        runCurrent()
        assertEquals(listOf(coffee()), model.state.value.items)
        assertFalse(model.state.value.isLoading)
        assertTrue(model.state.value.filtersLoading)
        repeat(3) { model.onAction(CoffeeListAction.Initialize) }; runCurrent()
        assertEquals(1, repository.searches.size)
        assertEquals(1, repository.filterCalls)
        gate.complete(Unit); runCurrent()
        assertEquals(groups(), model.state.value.filterGroups)
        assertFalse(model.state.value.filtersLoading)
    }

    @Test fun queryIsLimitedAndDebouncedWithLatestInputWinning() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("old")); runCurrent()
        advanceTimeBy(100)
        model.onAction(CoffeeListAction.QueryChanged("x".repeat(110))); runCurrent()
        assertEquals("x".repeat(100), model.state.value.query)
        assertTrue(model.state.value.items.isEmpty())
        assertEquals(0, model.state.value.page)
        advanceTimeBy(299); runCurrent(); assertEquals(1, repository.searches.size)
        advanceTimeBy(1); runCurrent()
        assertEquals(listOf("", "x".repeat(100)), repository.searches.map { it.query })
        assertEquals(1, repository.searches.last().page)
    }

    @Test fun refreshDuringDebounceImmediatelyUsesCurrentQuery() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("new")); runCurrent()
        model.onAction(CoffeeListAction.Refresh); runCurrent()
        advanceTimeBy(500); runCurrent()
        assertEquals(listOf("", "new"), repository.searches.map { it.query })
        assertFalse(model.state.value.isLoading)
    }

    @Test fun staleNonCancellableSuccessCannotOverwriteNewQuery() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { search = { call ->
            if (call.query == "old") withContext(NonCancellable) { gate.await() }
            Result.success(page(call.query.ifBlank { "initial" }))
        } }
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("old")); runCurrent(); advanceTimeBy(300); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("new")); runCurrent(); advanceTimeBy(300); runCurrent()
        assertEquals(listOf("new"), model.state.value.items.map { it.slug })
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("new"), model.state.value.items.map { it.slug })
        assertFalse(model.state.value.loadFailed)
    }

    @Test fun staleFailureCannotClearNewLoadingOrBecomeAnError() = runTest(dispatcher) {
        val oldGate = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { search = { call ->
            when (call.query) {
                "old" -> { withContext(NonCancellable) { oldGate.await() }; Result.failure(IllegalStateException("private payload")) }
                "new" -> { newGate.await(); Result.success(page("new")) }
                else -> Result.success(page())
            }
        } }
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("old")); runCurrent(); advanceTimeBy(300); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("new")); runCurrent(); advanceTimeBy(300); runCurrent()
        oldGate.complete(Unit); runCurrent()
        assertTrue(model.state.value.isLoading)
        assertFalse(model.state.value.loadFailed)
        newGate.complete(Unit); runCurrent()
        assertEquals(listOf("new"), model.state.value.items.map { it.slug })
    }

    @Test fun duplicateLoadMoreIsGuardedAndRowsAreDeduplicated() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { search = { call ->
            if (call.page == 1) Result.success(page("a", "b", total = 2))
            else { gate.await(); Result.success(page("b", "c", current = 2, total = 2)) }
        } }
        val model = model(repository); runCurrent()
        repeat(4) { model.onAction(CoffeeListAction.LoadMore) }; runCurrent()
        assertEquals(listOf(1, 2), repository.searches.map { it.page })
        assertTrue(model.state.value.isLoadingMore)
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("a", "b", "c"), model.state.value.items.map { it.slug })
        assertEquals(2, model.state.value.page)
        assertFalse(model.state.value.hasMore)
        model.onAction(CoffeeListAction.LoadMore); runCurrent(); assertEquals(2, repository.searches.size)
    }

    @Test fun pageFailureKeepsRowsAndRetriesThatSamePage() = runTest(dispatcher) {
        var fail = true
        val repository = TestCoffeeRepository().apply { search = { call -> when {
            call.page == 1 -> Result.success(page("a", total = 2))
            fail -> Result.failure(IllegalStateException("private payload"))
            else -> Result.success(page("b", current = 2, total = 2))
        } } }
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.LoadMore); runCurrent()
        assertTrue(model.state.value.loadMoreFailed)
        assertTrue(model.state.value.loadFailed)
        assertEquals(1, model.state.value.page)
        assertEquals(listOf("a"), model.state.value.items.map { it.slug })
        fail = false; model.onAction(CoffeeListAction.LoadMore); runCurrent()
        assertEquals(listOf(1, 2, 2), repository.searches.map { it.page })
        assertEquals(listOf("a", "b"), model.state.value.items.map { it.slug })
        assertFalse(model.state.value.loadFailed)
    }

    @Test fun refreshKeepsVisibleRowsAndReplacesThemOnSuccess() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        repository.search = { gate.await(); Result.success(page("fresh")) }
        model.onAction(CoffeeListAction.Refresh); runCurrent()
        assertTrue(model.state.value.isRefreshing)
        assertFalse(model.state.value.isLoading)
        assertEquals(listOf(coffee()), model.state.value.items)
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("fresh"), model.state.value.items.map { it.slug })
        assertFalse(model.state.value.isRefreshing)
    }

    @Test fun failedRefreshKeepsRowsAndStopsItsSpinner() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        repository.search = { Result.failure(IllegalStateException("private payload")) }
        model.onAction(CoffeeListAction.Refresh); runCurrent()
        assertEquals(listOf(coffee()), model.state.value.items)
        assertTrue(model.state.value.loadFailed)
        assertFalse(model.state.value.loadMoreFailed)
        assertFalse(model.state.value.isRefreshing)
    }

    @Test fun filterDraftCanBeCancelledWithoutARequestOrChangingAppliedValues() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.FilterToggled("taste", "fruit"))
        model.onAction(CoffeeListAction.AvailableOnlyChanged(true)); runCurrent()
        val draft = model.state.value.filterDraft
        assertEquals(2, draft?.activeCount)
        assertEquals(CoffeeFilters(), model.state.value.filters)
        model.onAction(CoffeeListAction.OpenFilters); runCurrent(); assertEquals(draft, model.state.value.filterDraft)
        model.onAction(CoffeeListAction.CancelFilters); runCurrent()
        assertNull(model.state.value.filterDraft)
        assertEquals(1, repository.searches.size)
        model.onAction(CoffeeListAction.OpenFilters); runCurrent()
        assertEquals(CoffeeFilters(), model.state.value.filterDraft)
    }

    @Test fun filterApplyAndResetSearchFromFirstPage() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val model = model(repository); runCurrent()
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.FilterToggled("taste", "fruit"))
        model.onAction(CoffeeListAction.AvailableOnlyChanged(true))
        model.onAction(CoffeeListAction.ApplyFilters); runCurrent()
        val applied = CoffeeFilters(mapOf("taste" to setOf("fruit")), true)
        assertEquals(applied, repository.searches.last().filters)
        assertEquals(applied, model.state.value.filters)
        assertNull(model.state.value.filterDraft)
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.ResetFilterDraft); runCurrent()
        assertEquals(applied, model.state.value.filters)
        model.onAction(CoffeeListAction.ApplyFilters); runCurrent()
        assertEquals(CoffeeFilters(), repository.searches.last().filters)
        assertEquals(listOf(1, 1, 1), repository.searches.map { it.page })
        model.onAction(CoffeeListAction.ApplyFilters); runCurrent(); assertEquals(3, repository.searches.size)
    }

    @Test fun closedDraftAndUnknownOptionsCannotChangeFilters() = runTest(dispatcher) {
        val model = model(); runCurrent()
        model.onAction(CoffeeListAction.AvailableOnlyChanged(true))
        model.onAction(CoffeeListAction.FilterToggled("taste", "fruit")); runCurrent()
        assertNull(model.state.value.filterDraft)
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.FilterToggled("unknown", "fruit"))
        model.onAction(CoffeeListAction.FilterToggled("taste", "unknown")); runCurrent()
        assertEquals(CoffeeFilters(), model.state.value.filterDraft)
    }

    @Test fun labelsFailureCanBeRetriedWithoutRefreshingSearch() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { filters = { Result.failure(IllegalStateException("private payload")) } }
        val model = model(repository); runCurrent()
        assertTrue(model.state.value.filtersFailed)
        assertFalse(model.state.value.filtersLoading)
        assertFalse(model.state.value.loadFailed)
        repository.filters = { Result.success(groups()) }
        model.onAction(CoffeeListAction.RetryFilters); runCurrent()
        assertEquals(groups(), model.state.value.filterGroups)
        assertFalse(model.state.value.filtersFailed)
        assertEquals(1, repository.searches.size)
    }

    @Test fun lateLabelsCannotOverwriteACompletedRetry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { filters = {
            withContext(NonCancellable) { gate.await() }; Result.success(emptyList())
        } }
        val model = model(repository); runCurrent()
        repository.filters = { Result.success(groups()) }
        model.onAction(CoffeeListAction.RetryFilters); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(groups(), model.state.value.filterGroups)
        assertFalse(model.state.value.filtersLoading)
    }

    @Test fun cancellationInResultIsNotRenderedAsFailureAndRetryStillWorks() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { search = { Result.failure(CancellationException("cancelled")) } }
        val model = model(repository); runCurrent()
        assertFalse(model.state.value.loadFailed)
        assertFalse(model.state.value.isLoading)
        repository.search = { Result.success(page()) }
        model.onAction(CoffeeListAction.Refresh); runCurrent()
        assertEquals(listOf(coffee()), model.state.value.items)
    }

    @Test fun clearingLifecyclePreventsLateRows() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { search = {
            withContext(NonCancellable) { gate.await() }; Result.success(page("late"))
        } }
        val model = model(repository); runCurrent()
        store.clear(); gate.complete(Unit); runCurrent()
        assertTrue(model.state.value.items.isEmpty())
        assertFalse(model.state.value.loadFailed)
    }

    @Test fun navigationEmitsOnlyForCurrentVisibleItems() = runTest(dispatcher) {
        val model = model(); runCurrent()
        val events = mutableListOf<CoffeeListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.onAction(CoffeeListAction.OpenCoffee("unknown"))
        model.onAction(CoffeeListAction.OpenCoffee(" "))
        model.onAction(CoffeeListAction.OpenCoffee("coffee")); runCurrent()
        assertEquals(listOf<CoffeeListEvent>(CoffeeListEvent.OpenCoffee("coffee")), events)
    }
}
