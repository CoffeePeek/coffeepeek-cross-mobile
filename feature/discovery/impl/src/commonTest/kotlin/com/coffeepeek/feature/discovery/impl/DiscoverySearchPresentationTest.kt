package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.Paged
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverySearchPresentationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeRepository()
    private val analytics = RecordingAnalytics()
    private val account = FakeAccount()
    private val favorites = FakeFavorites()
    private val city = FakeCity()
    private val location = FakeLocation()
    private lateinit var search: SearchViewModel
    private lateinit var map: MapViewModel

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.models() {
        search = SearchViewModel(repository, favorites, city, location, account, analytics)
        map = MapViewModel(repository, city, location, FakeShopDetails(), analytics, account, favorites)
        store.put("search", search)
        store.put("map", map)
        backgroundScope.launch { search.state.map { it.criteria }.distinctUntilChanged().collect { map.onAction(MapAction.SyncCriteria(it)) } }
        runCurrent()
    }

    private fun TestScope.present(listActive: Boolean = false, mapActive: Boolean = false) {
        search.onAction(SearchAction.PresentationChanged(listActive))
        map.onAction(MapAction.PresentationChanged(mapActive))
        runCurrent()
    }

    private fun TestScope.type(text: String) {
        search.onAction(SearchAction.Query(text))
        runCurrent()
        advanceTimeBy(501)
        runCurrent()
    }

    private fun clearSearchRecords() {
        repository.queries.clear()
        analytics.searches.clear()
        analytics.results.clear()
        analytics.failures.clear()
        analytics.mapSearches.clear()
        analytics.mapSearchResults.clear()
        analytics.mapSearchFailures.clear()
    }

    @Test fun listTypingKeepsCollapsedMapCriteriaWithoutSuggestionsOrMapTelemetry() = runTest(dispatcher) {
        models()
        present(listActive = true)
        clearSearchRecords()
        type("laptop")
        assertEquals("laptop", map.state.value.query)
        assertEquals(20, repository.queries.single().pageSize)
        assertEquals(1, analytics.searches.size)
        assertTrue(analytics.mapSearches.isEmpty())
        assertFalse(map.state.value.showSuggestions)
        assertTrue(map.state.value.suggestions.isEmpty())
    }

    @Test fun mapTypingRecordsOnlyMapSearchWhileSharingTheQueryAndListData() = runTest(dispatcher) {
        models()
        present(mapActive = true)
        clearSearchRecords()
        type("coffee")
        assertEquals("coffee", search.state.value.query)
        assertEquals("coffee", map.state.value.query)
        assertEquals(setOf(3, 20), repository.queries.map { it.pageSize }.toSet())
        assertTrue(repository.queries.all { it.text == "coffee" })
        assertTrue(analytics.searches.isEmpty())
        assertEquals(1, analytics.mapSearches.size)
        assertEquals(listOf(2), analytics.mapSearchResults)
    }

    @Test fun openingMapSearchesThePreservedQueryOnceWhenSuggestionsBecomeVisible() = runTest(dispatcher) {
        models()
        present(listActive = true)
        type("laptop")
        clearSearchRecords()
        present(mapActive = true)
        advanceTimeBy(351)
        runCurrent()
        present(mapActive = true)
        assertEquals("laptop", repository.queries.single().text)
        assertEquals(3, repository.queries.single().pageSize)
        assertEquals(1, analytics.mapSearches.size)
        assertTrue(analytics.searches.isEmpty())
        assertTrue(map.state.value.showSuggestions)
    }

    @Test fun closingMapBeforeDebounceCancelsSuggestionsAndPreservesTheQuery() = runTest(dispatcher) {
        models()
        present(mapActive = true)
        clearSearchRecords()
        search.onAction(SearchAction.Query("coffee"))
        runCurrent()
        present()
        advanceTimeBy(501)
        runCurrent()
        assertEquals("coffee", map.state.value.query)
        assertFalse(map.state.value.isSearchLoading)
        assertFalse(map.state.value.showSuggestions)
        assertTrue(repository.queries.none { it.pageSize == 3 })
        assertTrue(analytics.mapSearches.isEmpty())
        assertTrue(analytics.searches.isEmpty())
    }

    @Test fun hiddenListStillDeduplicatesNormalizedDataRequests() = runTest(dispatcher) {
        models()
        clearSearchRecords()
        type("coffee")
        type("  coffee  ")
        assertEquals(1, repository.queries.size)
        assertTrue(analytics.searches.isEmpty())
        assertTrue(analytics.mapSearches.isEmpty())
    }

    @Test fun equivalentQueryAndUnusedOriginChangesKeepTheVisibleSearchOutcome() = runTest(dispatcher) {
        models()
        present(listActive = true)
        clearSearchRecords()
        val reply = CompletableDeferred<Result<Paged<ShopSummary>>>()
        repository.search = { reply.await() }
        type("coffee")
        type("  coffee  ")
        location.flow.value = Coordinates(53.9, 27.56)
        runCurrent()
        reply.complete(page(listOf(shop("a"))))
        runCurrent()
        assertEquals(1, repository.queries.size)
        assertEquals(1, analytics.searches.size)
        assertEquals(listOf(1), analytics.results)
    }

    @Test fun hiddenListDoesNotReportLateSuccess() = runTest(dispatcher) {
        lateListOutcome(page(listOf(shop("late"))))
        assertTrue(analytics.results.isEmpty())
        assertEquals("late", search.state.value.shops.single().id)
    }

    @Test fun hiddenListDoesNotReportLateFailure() = runTest(dispatcher) {
        lateListOutcome(Result.failure(IllegalStateException("offline")))
        assertTrue(analytics.failures.isEmpty())
        assertNotNull(search.state.value.error)
    }

    private suspend fun TestScope.lateListOutcome(result: Result<Paged<ShopSummary>>) {
        models()
        present(listActive = true)
        clearSearchRecords()
        val reply = CompletableDeferred<Result<Paged<ShopSummary>>>()
        repository.search = { reply.await() }
        type("coffee")
        assertEquals(1, analytics.searches.size)
        present()
        reply.complete(result)
        runCurrent()
        assertTrue(analytics.mapSearches.isEmpty())
    }

    @Test fun hiddenMapIgnoresLateFailureEvenWhenTheRequestIgnoresCancellation() = runTest(dispatcher) {
        lateMapOutcome(Result.failure(IllegalStateException("late offline")))
    }

    @Test fun hiddenMapIgnoresLateSuccessEvenWhenTheRequestIgnoresCancellation() = runTest(dispatcher) {
        lateMapOutcome(page(listOf(shop("late"))))
    }

    private suspend fun TestScope.lateMapOutcome(result: Result<Paged<ShopSummary>>) {
        models()
        present(mapActive = true)
        clearSearchRecords()
        val reply = CompletableDeferred<Result<Paged<ShopSummary>>>()
        repository.search = { query -> if (query.pageSize == 3) withContext(NonCancellable) { reply.await() } else page(emptyList()) }
        type("coffee")
        assertEquals(1, analytics.mapSearches.size)
        present()
        reply.complete(result)
        runCurrent()
        assertTrue(analytics.mapSearchResults.isEmpty())
        assertTrue(analytics.mapSearchFailures.isEmpty())
        assertFalse(map.state.value.showSuggestions)
        assertFalse(map.state.value.searchFailed)
        assertTrue(map.state.value.suggestions.isEmpty())
    }
}
