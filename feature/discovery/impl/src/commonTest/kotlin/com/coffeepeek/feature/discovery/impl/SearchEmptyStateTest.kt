package com.coffeepeek.feature.discovery.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

class SearchEmptyStateTest {
    private val ready = SearchState(isLoading = false)

    @Test fun emptyCatalogQueryAndFiltersHaveDistinctRecovery() {
        assertEquals(SearchEmptyReason.Catalog, ready.emptyReason)
        assertEquals(SearchEmptyReason.Query, ready.copy(query = "missing").emptyReason)
        assertEquals(SearchEmptyReason.Filters, ready.copy(query = "missing", filters = DiscoveryFilters(openOnly = true)).emptyReason)
        assertNull(ready.copy(shops = listOf(ShopSummary("shop", "Coffee"))).emptyReason)
    }

    @Test fun pendingPagesAndFailuresNeverBecomeEmptyOrContributionPrompts() {
        val pending = listOf(ready.copy(isLoading = true), ready.copy(isRefreshing = true),
            ready.copy(isLoadingMore = true), ready.copy(hasMore = true),
            ready.copy(error = SearchError(null)), ready.copy(loadMoreError = SearchError(null)))
        pending.forEach {
            assertNull(it.emptyReason)
            assertFalse(it.canSuggestShop)
        }
        assertTrue(ready.canSuggestShop)
    }

    @Test fun globalNoMatchesWaitsForBothSuccessfulSearches() {
        val text = ready.copy(query = "missing")
        assertTrue(text.hasNoSearchMatches(false))
        assertFalse(text.hasNoSearchMatches(null))
        assertFalse(text.hasNoSearchMatches(true))
        assertFalse(text.copy(error = SearchError(null)).hasNoSearchMatches(false))
        assertFalse(ready.hasNoSearchMatches(false))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun resetFiltersKeepsQueryAndCityAndReloadsOnce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val repository = FakeRepository()
            val vm = SearchViewModel(repository, FakeFavorites(), FakeCity(), FakeLocation(), FakeAccount(), RecordingAnalytics())
            store.put("search", vm)
            vm.onAction(SearchAction.Query("Coffee"))
            vm.onAction(SearchAction.ToggleOpen)
            advanceUntilIdle()
            val before = repository.queries.size
            vm.onAction(SearchAction.ResetFilterSelections)
            advanceUntilIdle()
            assertEquals("Coffee", vm.state.value.query)
            assertEquals(DiscoveryFilters(cityId = "minsk"), vm.state.value.filters)
            assertEquals(before + 1, repository.queries.size)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
