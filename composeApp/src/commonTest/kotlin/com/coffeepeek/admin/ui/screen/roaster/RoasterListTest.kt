package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.feature.catalog.ui.roasterCountLabel
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterLocation
import com.coffeepeek.domain.model.RoasterShop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoasterListTest {
    @Test
    fun searchesWithoutFilteringOutRoastersWithMissingDetails() {
        val kitchen = RoasterListItem(CatalogItem("k", "Kitchen"), RoasterDetails(
            "k", "Kitchen", location = RoasterLocation("Nearby", 0.001, 0.0), shops = listOf(RoasterShop("s", "Shop")),
        ))
        val river = RoasterListItem(CatalogItem("r", "River"), RoasterDetails(
            "r", "River", location = RoasterLocation("Far", 1.0, 0.0), shops = List(3) { RoasterShop("$it", "Shop") },
        ))
        val unknown = RoasterListItem(CatalogItem("u", "Unknown"))
        val state = RoasterListUiState(items = listOf(kitchen, river, unknown))
        assertEquals(listOf(kitchen), state.copy(query = "  KITCH  ").visibleItems)
        assertEquals(listOf(kitchen, river, unknown), state.visibleItems)
        assertEquals(state.items, state.copy(query = "   ").visibleItems)
        assertEquals(listOf(unknown), state.copy(query = "unknown").visibleItems)
        assertTrue(state.copy(query = "missing").visibleItems.isEmpty())
        assertEquals(state.items, state.copy(query = "Kitchen").copy(query = "").visibleItems)
        assertNull(unknown.routeId)
        assertNull(RoasterListItem(CatalogItem("", "", address = PublicAddress("", "", 0, false))).routeId)
    }

    @Test
    fun displaysServerCountsAndDashForZeroOrMissingCounts() {
        val item = RoasterListItem(
            CatalogItem("r", "Roast", coffeeShopsCount = 7, coffeeProductsCount = 12),
            RoasterDetails("r", "Roast", shops = listOf(RoasterShop("s", "Shop"))),
        )
        assertEquals("7", roasterCountLabel(item.catalog.coffeeShopsCount))
        assertEquals("12", roasterCountLabel(item.catalog.coffeeProductsCount))
        assertEquals("—", roasterCountLabel(0))
        assertEquals("—", roasterCountLabel(CatalogItem("empty", "Empty").coffeeProductsCount))
        assertEquals("—", roasterCountLabel(-1))
    }
}
