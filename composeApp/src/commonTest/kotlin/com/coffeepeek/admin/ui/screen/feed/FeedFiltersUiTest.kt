package com.coffeepeek.admin.ui.screen.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

class FeedFiltersUiTest {

    @Test
    fun previewFollowsScrollAndStaysAboveTheViewportWhenItsItemIsRecycled() {
        val anchor = Rect(16f, 180f, 384f, 420f)
        assertEquals(anchor, discoveryMapPreviewBounds(anchor, 164f, 16))
        val scrolledAnchor = Rect(16f, 64f, 384f, 304f)
        assertEquals(scrolledAnchor, discoveryMapPreviewBounds(scrolledAnchor, 164f, -100))
        val pulledAnchor = Rect(16f, 252f, 384f, 492f)
        assertEquals(pulledAnchor, discoveryMapPreviewBounds(pulledAnchor, 164f, 16))
        val recycled = discoveryMapPreviewBounds(anchor, 164f, null)
        assertEquals(164f, recycled.bottom)
        assertEquals(anchor, discoveryMapPreviewBounds(anchor, 164f, 16))
    }

    @Test
    fun shopListPushesTheMapWithoutOverlappingItAndCancelReversesTheMotion() {
        val height = 800f
        for (progress in listOf(1f, 0.75f, 0.5f, 0.25f, 0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val mapOffset = discoveryMapOffsetY(height, progress)
            val incomingListTop = height * progress
            assertEquals(incomingListTop, height + mapOffset)
            assertTrue(240f + mapOffset <= incomingListTop)
        }
        assertEquals(-height, discoveryMapOffsetY(height, -1f))
        assertEquals(0f, discoveryMapOffsetY(height, 2f))
    }

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
