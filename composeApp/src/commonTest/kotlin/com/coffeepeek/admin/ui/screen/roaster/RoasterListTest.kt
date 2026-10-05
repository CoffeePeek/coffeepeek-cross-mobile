package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.feature.catalog.ui.roasterShopCountLabel
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterLocation
import com.coffeepeek.domain.model.RoasterShop
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(listOf(unknown), state.copy(query = "unknown").visibleItems)
        assertTrue(state.copy(query = "missing").visibleItems.isEmpty())
        assertEquals("1 кофейня использует это зерно", roasterShopCountLabel(1))
        assertEquals("12 кофеен используют это зерно", roasterShopCountLabel(12))
        assertEquals("22 кофейни используют это зерно", roasterShopCountLabel(22))
    }
}
