package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.domain.model.CoffeeShop
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedSearchResultOrderTest {
    @Test fun onlyMatchingShopsPrecedeAConfirmedEmptyRoasterSection() {
        val state = FeedUiState(query = "cafe", isLoading = false, shops = listOf(CoffeeShop(
            id = "cafe", title = "Cafe", rating = null, cityName = "", priceRange = null, photoUrl = null,
            isFavorite = false, isVisited = false, isOpen = false, isNew = false,
        )))
        assertTrue(state.showsShopsFirst(false))
        assertFalse(state.showsShopsFirst(true))
        assertFalse(state.showsShopsFirst(null))
        assertFalse(state.copy(shops = emptyList()).showsShopsFirst(false))
        assertFalse(state.copy(query = " ").showsShopsFirst(false))
        assertFalse(state.copy(filters = FeedFiltersUi(favoritesOnly = true)).showsShopsFirst(false))
    }
}
