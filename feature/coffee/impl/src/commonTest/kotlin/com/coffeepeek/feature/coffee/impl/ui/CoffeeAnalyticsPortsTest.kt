package com.coffeepeek.feature.coffee.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.coffee.api.*
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.impl.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

private class RecordingList : CoffeeListAnalytics {
    val calls = mutableListOf<String>()
    override fun searchStarted(fingerprint: String, query: String, filterKeys: List<String>) { calls += "start:$fingerprint:$filterKeys" }
    override fun resultsViewed(count: Int) { calls += "results:$count" }
    override fun searchFailed(error: Throwable) { calls += "failed" }
    override fun opened(position: Int) { calls += "open:$position" }
    override fun filtersOpened() { calls += "filters-open" }
    override fun filtersApplied(filterKeys: List<String>, changedKeys: List<String>, reset: Boolean) {
        calls += "applied:$filterKeys:$changedKeys:$reset"
    }
}

private class RecordingDetail : CoffeeDetailAnalytics {
    val calls = mutableListOf<String>()
    override fun contentInteracted(section: CoffeeDetailSection, action: CoffeeContentAction) { calls += "$section:$action" }
    override fun roasterOpened() { calls += "roaster" }
    override fun similarOpened() { calls += "similar" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeAnalyticsPortsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun list(repository: TestCoffeeRepository, analytics: RecordingList) =
        CoffeeListViewModel(repository, analytics).also { store.put("list-${analytics.hashCode()}", it) }

    @Test fun listReportsStartResultsOpenAndFilterFlowWithoutTextOrSlugs() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { search = { Result.success(page("a", "b")) } }
        val analytics = RecordingList()
        val model = list(repository, analytics); runCurrent()
        assertEquals(listOf("start:||false:[]", "results:2"), analytics.calls)
        model.onAction(CoffeeListAction.OpenCoffee("b")); runCurrent()
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.OpenFilters) // already open: no second event
        runCurrent()
        analytics.calls.clear()
        model.onAction(CoffeeListAction.ApplyFilters); runCurrent()
        assertEquals("applied:[]:[]:true", analytics.calls.first())
    }

    @Test fun openedPositionIsOneBasedAndUnknownSlugIsIgnored() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply { search = { Result.success(page("a", "b")) } }
        val analytics = RecordingList()
        val model = list(repository, analytics); runCurrent()
        analytics.calls.clear()
        model.onAction(CoffeeListAction.OpenCoffee("missing")); runCurrent()
        model.onAction(CoffeeListAction.OpenCoffee("b")); runCurrent()
        assertEquals(listOf("open:2"), analytics.calls)
    }

    @Test fun appliedFiltersReportActiveKeysChangesAndReset() = runTest(dispatcher) {
        val repository = TestCoffeeRepository()
        val analytics = RecordingList()
        val model = list(repository, analytics); runCurrent()
        model.onAction(CoffeeListAction.OpenFilters)
        model.onAction(CoffeeListAction.FilterToggled("taste", "fruit"))
        model.onAction(CoffeeListAction.AvailableOnlyChanged(true)); runCurrent()
        analytics.calls.clear()
        model.onAction(CoffeeListAction.ApplyFilters); runCurrent()
        assertEquals("applied:[availableOnly, taste]:[availableOnly, taste]:false", analytics.calls.first())
        assertTrue(analytics.calls[1].startsWith("start:|taste=fruit|true:[taste, availableOnly]"))
    }

    @Test fun failureAndStaleResultsAreReportedOnceAndLoadMoreDoesNotRestartSearch() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply { search = { call ->
            when {
                call.query == "boom" -> Result.failure(IllegalStateException("payload"))
                call.query == "old" -> { withContext(NonCancellable) { gate.await() }; Result.success(page("old")) }
                call.page == 2 -> Result.success(page("c", current = 2, total = 2))
                else -> Result.success(page("a", total = 2))
            }
        } }
        val analytics = RecordingList()
        val model = list(repository, analytics); runCurrent()
        model.onAction(CoffeeListAction.LoadMore); runCurrent()
        assertEquals(listOf("start:||false:[]"), analytics.calls.filter { it.startsWith("start") })
        assertEquals(0, analytics.calls.count { it == "failed" })
        // Stale: "old" is superseded before it returns, so it must not report results.
        model.onAction(CoffeeListAction.QueryChanged("old")); runCurrent(); advanceTimeBy(300); runCurrent()
        model.onAction(CoffeeListAction.QueryChanged("boom")); runCurrent(); advanceTimeBy(300); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(1, analytics.calls.count { it == "failed" })
        // The superseded "old" search never reports results after the newer failure.
        assertFalse(analytics.calls.lastIndexOf("results:1") > analytics.calls.indexOf("failed"))
    }

    @Test fun detailReportsContentInteractionsAndOnlyForActionableTargets() = runTest(dispatcher) {
        val repository = TestCoffeeRepository().apply {
            detail = { Result.success(detail(coffee("c", listOf("fruit")))) }
            search = { Result.success(page("c", "s")) }
        }
        val analytics = RecordingDetail()
        val model = CoffeeDetailViewModel("c", repository, analytics).also { store.put("d", it) }; runCurrent()
        model.onAction(CoffeeDetailAction.OpenPhotos)
        model.onAction(CoffeeDetailAction.ToggleDescription)
        model.onAction(CoffeeDetailAction.ToggleDescription)
        model.onAction(CoffeeDetailAction.OpenRoaster)
        model.onAction(CoffeeDetailAction.OpenSimilar("unknown"))
        model.onAction(CoffeeDetailAction.OpenSimilar("s"))
        model.onAction(CoffeeDetailAction.Share); runCurrent()
        assertEquals(listOf("Photos:Open", "Description:Open", "Description:Close", "Roaster:Select", "roaster",
            "SimilarCoffees:Select", "similar", "Contacts:Share"), analytics.calls)
    }
}
