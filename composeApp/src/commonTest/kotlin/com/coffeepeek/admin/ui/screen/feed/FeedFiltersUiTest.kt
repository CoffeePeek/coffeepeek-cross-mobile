package com.coffeepeek.admin.ui.screen.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

class FeedFiltersUiTest {

    @Test
    fun mapExpandsFromItsPreviewBoundsToTheWholeScreen() {
        val preview = Rect(16f, 180f, 384f, 420f)
        val screen = Size(400f, 800f)
        assertEquals(preview, expandedMapBounds(preview, screen, 0f))
        assertEquals(Rect(8f, 90f, 392f, 610f), expandedMapBounds(preview, screen, 0.5f))
        assertEquals(Rect(0f, 0f, 400f, 800f), expandedMapBounds(preview, screen, 1f))
    }

    @Test
    fun discoveryStaysHiddenDuringEmptySearchAndAfterQueryLosesFocus() {
        val initial = FeedUiState()
        assertTrue(initial.showDiscovery)
        assertFalse(initial.copy(isSearchActive = true).showDiscovery)
        assertFalse(initial.copy(query = "Coffee", isSearchActive = false).showDiscovery)
        assertTrue(initial.copy(query = "", isSearchActive = false).showDiscovery)
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
