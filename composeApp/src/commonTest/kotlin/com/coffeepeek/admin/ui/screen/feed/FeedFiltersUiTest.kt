package com.coffeepeek.admin.ui.screen.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedFiltersUiTest {

    @Test
    fun discoveryStaysHiddenDuringEmptySearchAndAfterQueryLosesFocus() {
        val initial = FeedUiState()
        assertTrue(initial.showDiscovery)
        assertFalse(initial.copy(isSearchActive = true).showDiscovery)
        assertFalse(initial.copy(query = "Coffee", isSearchActive = false).showDiscovery)
        assertTrue(initial.copy(query = "", isSearchActive = false).showDiscovery)
    }

    @Test
    fun nearbyFilterKeepsDiscoveryWithoutClosingAnActiveSearch() {
        val nearby = FeedUiState(filters = FeedFiltersUi(cityId = "minsk", nearbyOnly = true))

        assertTrue(nearby.showDiscovery)
        assertEquals(1, nearby.activeFilterCount)
        assertTrue(nearby.copy(filters = nearby.filters.copy(nearbyOnly = false)).showDiscovery)
        assertFalse(nearby.copy(isSearchActive = true).showDiscovery)
        assertFalse(nearby.copy(query = "Coffee").showDiscovery)
    }

    @Test
    fun otherSelectedFiltersOpenTheShopListWhileTheDefaultCityKeepsDiscovery() {
        val initial = FeedUiState(filters = FeedFiltersUi(cityId = "minsk"))
        assertTrue(initial.showDiscovery)
        listOf(
            FeedFiltersUi(coffeeFocus = "specialty"),
            FeedFiltersUi(openOnly = true),
            FeedFiltersUi(newOnly = true),
            FeedFiltersUi(visitedOnly = true),
            FeedFiltersUi(favoritesOnly = true),
            FeedFiltersUi(priceRange = 2),
            FeedFiltersUi(minRating = 4.0),
            FeedFiltersUi(roasterIds = setOf("roaster")),
            FeedFiltersUi(beanIds = setOf("bean")),
            FeedFiltersUi(equipmentIds = setOf("equipment")),
            FeedFiltersUi(brewMethodIds = setOf("brew")),
            FeedFiltersUi(tagIds = setOf("wifi")),
        ).forEach { filters ->
            val filtered = initial.copy(filters = filters.copy(cityId = "minsk"))
            assertFalse(filtered.showDiscovery, filters.toString())
            assertFalse(filtered.copy(filters = filtered.filters.copy(nearbyOnly = true)).showDiscovery, filters.toString())
            assertTrue(filtered.copy(filters = filtered.filters.clearSelections()).showDiscovery)
        }
    }

    @Test
    fun activeFilterCountIncludesQuickAndAdvancedFilters() {
        val filters = FeedFiltersUi(
            cityId = "minsk",
            coffeeFocus = "specialty",
            openOnly = true,
            roasterIds = setOf("roaster-1"),
        )

        assertEquals(3, filters.activeFilterCount)
    }

    @Test
    fun clearSelectionsRemovesQuickAndAdvancedFiltersButKeepsCity() {
        val filters = FeedFiltersUi(
            cityId = "minsk",
            coffeeFocus = "specialty",
            openOnly = true,
            newOnly = true,
            nearbyOnly = true,
            priceRange = 2,
            roasterIds = setOf("roaster-1"),
            beanIds = setOf("bean-1"),
            tagIds = setOf("wifi"),
        )

        assertEquals(FeedFiltersUi(cityId = "minsk"), filters.clearSelections())
        assertEquals(0, filters.clearSelections().activeFilterCount)
    }
}
