package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.feature.catalog.ui.roasterCountLabel
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterLocation
import com.coffeepeek.domain.model.RoasterShop
import com.coffeepeek.domain.model.RoasterSummary
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
        assertEquals(listOf(kitchen), state.visibleItems("  KITCH  "))
        assertEquals(listOf(kitchen, river, unknown), state.visibleItems(""))
        assertEquals(state.items, state.visibleItems("   "))
        assertEquals(listOf(unknown), state.visibleItems("unknown"))
        assertTrue(state.visibleItems("missing").isEmpty())
        assertEquals(state.items, state.visibleItems(""))
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

    @Test
    fun combinesNameSelectedRoastersAndFavoritesLocally() {
        val kitchen = RoasterListItem(CatalogItem("k", "Kitchen"))
        val other = RoasterListItem(CatalogItem("o", "Other Kitchen"))
        val state = RoasterListUiState(items = listOf(kitchen, other), favoriteIds = setOf("k"))
        assertEquals(listOf(other), state.visibleItems("kitch", selectedRoasterIds = setOf("o")))
        assertEquals(listOf(kitchen), state.visibleItems("kitch", favoritesOnly = true))
        assertTrue(state.visibleItems("kitch", selectedRoasterIds = setOf("o"), favoritesOnly = true).isEmpty())
        assertEquals(listOf(kitchen, other), state.visibleItems("  "))
    }

    @Test
    fun usesTheSharedQueryAndRealSummaryTagsWithoutRequiringDetails() {
        val light = CatalogItem("light", "Светлая", slug = "light")
        val decaf = CatalogItem("decaf", "Декаф", slug = "decaf")
        val roast = RoasterListItem(RoasterSummary(
            PublicAddress("roast", "/roasters/roast", 1, false), "Обжарщик Roast",
            tags = listOf(light, decaf), coffeeShopsCount = 7, coffeeProductsCount = 21,
        ))
        val other = RoasterListItem(RoasterSummary(
            PublicAddress("other", "/roasters/other", 1, false), "Другой обжарщик",
            tags = listOf(light), coffeeShopsCount = 3,
        ))
        val all = RoasterListUiState(items = listOf(roast, other))
        assertEquals(listOf(roast, other), all.visibleItems("  ОБЖАРЩИК  "))
        assertEquals(listOf(roast), all.visibleItems("roast"))
        val filtered = all.copy(selectedTagIds = setOf("light", "decaf"))
        assertEquals(listOf(roast), filtered.visibleItems("обжарщик"))
        assertTrue(filtered.visibleItems("другой").isEmpty())
        assertEquals(listOf(roast), all.visibleItems("", selectedRoasterIds = setOf("roast")))
        assertEquals(setOf("light", "decaf"), all.availableTags.map { it.slug }.toSet())
        assertEquals("7", roasterCountLabel(roast.catalog.coffeeShopsCount))
        assertEquals("21", roasterCountLabel(roast.catalog.coffeeProductsCount))
        assertEquals(listOf(light, decaf), roast.catalog.tags)
    }
}
