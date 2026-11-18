package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.impl.map.MapAction
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.search.*
import kotlin.test.*

class DiscoveryIosIntentsTest {
    @Test fun pickerKeepsAcceptedRowsWhileNewFiltersArePending() {
        val favorite = ShopSummary("favorite", "Favorite", isFavorite = true)
        val ordinary = ShopSummary("ordinary", "Ordinary")
        val state = SearchState(shops = listOf(favorite, ordinary), filters = DiscoveryFilters(favoritesOnly = true),
            pickerResultCriteria = DiscoveryCriteria("", DiscoveryFilters(), null))
        assertEquals(listOf("favorite", "ordinary"), iosShopRows(state, picker = true).map { it.id })
        assertEquals(listOf("favorite"), iosShopRows(state, picker = false).map { it.id })
    }

    @Test fun pickerRecentsDisappearWhenQueryOrNearbyCriteriaArePending() {
        val state = SearchState(pickerRecentShops = listOf(ShopSummary("recent", "Recent")),
            pickerResultCriteria = DiscoveryCriteria("", DiscoveryFilters(), null))
        assertEquals(listOf("recent"), iosRecentRows(state, true).map { it.id })
        assertTrue(iosRecentRows(state.copy(query = "coffee"), true).isEmpty())
        assertTrue(iosRecentRows(state.copy(filters = DiscoveryFilters(nearbyOnly = true)), true).isEmpty())
        assertTrue(iosRecentRows(state, false).isEmpty())
    }

    @Test fun pickerCannotPaginateAnotherQueryOrAnUnacceptedInitialPage() {
        val accepted = DiscoveryCriteria("coffee", DiscoveryFilters(), null)
        val state = SearchState(isLoading = false, hasMore = true, query = "coffee", pickerResultCriteria = accepted)
        assertEquals(SearchAction.LoadMore, discoveryIosSearchIntent("more", "", state, picker = true))
        assertNull(discoveryIosSearchIntent("more", "", state.copy(query = "tea"), picker = true))
        assertNull(discoveryIosSearchIntent("more", "", state.copy(pickerResultCriteria = null), picker = true))
        assertNull(discoveryIosSearchIntent("more", "", state.copy(isLoadingMore = true), picker = true))
        assertEquals(SearchAction.LoadMore, discoveryIosSearchIntent("more", "", state.copy(pickerResultCriteria = null)))
    }

    @Test fun nativeOpenAcceptsOnlyCurrentResultOrRecentShops() {
        val state = SearchState(shops = listOf(ShopSummary("shop", "Shop")),
            pickerRecentShops = listOf(ShopSummary("recent", "Recent")))
        assertEquals(SearchAction.OpenShop("shop"), discoveryIosSearchIntent("open", "shop", state))
        assertEquals(SearchAction.OpenShop("recent"), discoveryIosSearchIntent("open", "recent", state))
        assertNull(discoveryIosSearchIntent("open", "obsolete", state))
        assertNull(discoveryIosSearchIntent("favorite", "obsolete", state))
    }

    @Test fun kindChipTogglesExistingFilterAndCancelUsesSharedSearchReset() {
        val selected = SearchState(filters = DiscoveryFilters(kind = ShopKind.Specialty))
        assertEquals(SearchAction.SetKind(null), discoveryIosSearchIntent("Specialty", "", selected))
        assertEquals(SearchAction.SetKind(ShopKind.Cafe), discoveryIosSearchIntent("Cafe", "", selected))
        assertEquals(SearchAction.CancelSearch, discoveryIosSearchIntent("cancel", "", selected))
        assertEquals(SearchAction.ToggleFavorites, discoveryIosSearchIntent("favorites", "", selected))
    }

    @Test fun recreatedNativeMapCanRestoreSharedViewportWithoutIssuingAnotherFocusAction() {
        val state = MapState(activeBounds = MapBounds(52.0, 27.0, 54.0, 29.0), activeZoom = 12f)
        assertEquals(Coordinates(53.0, 28.0), state.iosViewportCamera()?.target)
        assertEquals(12f, state.iosViewportCamera()?.zoom)
        assertNull(state.copy(activeZoom = null).iosViewportCamera())
        assertNull(state.copy(activeBounds = null).iosViewportCamera())
    }

    @Test fun nativeMapKeepsSelectionAndOpeningAsSeparateSharedIntents() {
        assertEquals(MapAction.SelectMarker("shop"), discoveryIosMapIntent("map-select", "shop"))
        assertEquals(MapAction.OpenShop("shop"), discoveryIosMapIntent("map-open", "shop"))
        assertEquals(MapAction.SelectFromCarousel("shop"), discoveryIosMapIntent("carousel", "shop"))
        assertEquals(MapAction.SelectSuggestion("shop"), discoveryIosMapIntent("suggestion", "shop"))
        assertEquals(MapAction.ShowZoneShops, discoveryIosMapIntent("zone-shops", ""))
        assertNull(discoveryIosMapIntent("unknown", "shop"))
    }
}
