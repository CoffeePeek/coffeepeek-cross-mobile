package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.impl.search.SearchState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultOrderTest {
    private val found = SearchState(query = "cafe", isLoading = false,
        shops = listOf(ShopSummary("cafe", "Cafe")))

    @Test fun matchingShopsPrecedeAnEmptyRoasterSection() {
        assertTrue(found.showsShopsFirst(hasRoasterResults = false))
    }

    @Test fun matchingRoastersKeepTheirExistingPriority() {
        assertFalse(found.showsShopsFirst(hasRoasterResults = true))
    }

    @Test fun loadingOrUnavailableRoastersDoNotCountAsAnEmptyResult() {
        assertFalse(found.showsShopsFirst(hasRoasterResults = null))
    }

    @Test fun emptyShopsAndBlankQueriesKeepTheirExistingOrder() {
        assertFalse(found.copy(shops = emptyList()).showsShopsFirst(hasRoasterResults = false))
        assertFalse(found.copy(query = "  ").showsShopsFirst(hasRoasterResults = false))
        assertFalse(found.copy(filters = DiscoveryFilters(favoritesOnly = true)).showsShopsFirst(hasRoasterResults = false))
    }
}
