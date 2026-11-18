package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.rules.distanceMeters
import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.domain.rules.visibleShops
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.canvasClusters
import com.coffeepeek.feature.discovery.impl.map.canvasHeatmap
import com.coffeepeek.feature.discovery.impl.search.*
import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.discovery.impl.ui.compose.component.intervalLabel

@Composable
internal fun discoveryIosState(search: SearchState, map: MapState, ui: DiscoveryTabState,
    picker: Boolean, active: Boolean, revision: Int, clearance: Float, canContinue: Boolean, hasRoasters: Boolean?): DiscoveryIosState {
    val rows = iosShopRows(search, picker)
    val recents = iosRecentRows(search, picker)
    val recentIds = recents.map { it.id }.toSet()
    return DiscoveryIosState(
        picker, active, search.query, search.showsDiscovery, ui.mapExpanded, ui.showRoasters,
        !picker && search.query.isNotBlank() && hasRoasters != false,
        !picker && search.query.isNotBlank() && hasRoasters == false && search.canSuggestShop, revision, clearance,
        search.isLoading, search.isRefreshing, search.isLoadingMore,
        search.canLoadMore && (!picker || search.pickerResultCriteria?.matchesPickerQuery(search.criteria) == true),
        search.error != null, search.loadMoreError != null, !picker && search.canSuggestShop,
        search.emptyReason?.name, iosLocationNotice(search, map, ui.mapExpanded),
        search.locationSettingsNeeded || (ui.mapExpanded && map.locationSettingsNeeded), search.favoriteFailureId != null,
        canContinue && !ui.mapExpanded && !ui.showRoasters,
        rows.filterNot { it.id in recentIds }.map { iosShop(it, search) }, recents.map { iosShop(it, search) },
        iosMapRows(map, search), map.selectedShop?.id,
        map.suggestions.map { iosShop(it, search) }, map.showSuggestions, map.searchFailed, map.isSearchLoading,
        map.selectedZone?.let { DiscoveryIosZone(it.name, it.description, it.shopCount) },
        map.showZones, map.showHeatmap, map.isHeatmapLoading, map.heatmapFailed,
        map.viewportFailed, map.isTruncated, iosChips(search), search.activeFilterCount, discoveryIosLabels(picker),
    )
}

internal fun iosShopRows(state: SearchState, picker: Boolean): List<ShopSummary> =
    if (picker) visibleShops(state.shops, state.pickerResultCriteria?.filters ?: state.filters).distinctBy { it.id }
    else state.visibleShops

internal fun iosRecentRows(state: SearchState, picker: Boolean): List<ShopSummary> {
    val applied = state.pickerResultCriteria
    val browsing = searchText(state.query) == null && !state.filters.nearbyOnly &&
        searchText(applied?.text.orEmpty()) == null && applied?.filters?.nearbyOnly != true
    return if (picker && browsing) state.pickerRecentShops.distinctBy { it.id } else emptyList()
}

@Composable
private fun iosShop(shop: ShopSummary, state: SearchState): DiscoveryIosShop {
    val distance = shop.distanceMeters ?: state.userLocation?.let { user -> shop.coordinates?.let { distanceMeters(user, it) } }
    return DiscoveryIosShop(shop.id, shop.title, shop.photoUrl, shop.address,
        shop.rating?.takeIf { it > 0 }?.let(::oneDecimal),
        if (shop.checkInCount > 0) stringResource(Res.string.discovery_map_card_checkins, shop.checkInCount) else null,
        distance?.let { distanceLabel(it) }, stringResource(when (shop.kind) {
            ShopKind.Specialty -> Res.string.discovery_shop_kind_specialty
            ShopKind.CoffeeBar -> Res.string.discovery_shop_kind_coffee_bar
            ShopKind.Cafe -> Res.string.discovery_shop_kind_cafe
        }), shop.tags, shop.isFavorite, shop.id in state.favoriteUpdates,
        stringResource(if (shop.isFavorite) Res.string.discovery_favorite_remove else Res.string.discovery_favorite_add),
        shop.isOpen, shop.isNew, null, DiscoveryIosShopDecoration(shop.priceLevel?.level ?: 0,
            shop.checkInCount, shop.brewMethods, shop.roasterLogos))
}

@Composable
private fun iosMapRows(map: MapState, search: SearchState): List<DiscoveryIosShop> = iosMapShops(map).map {
    val details = map.cardDetails[it.id] ?: map.details.takeIf { _ -> it.id == map.selectedShop?.id }
    iosShop(it, search).copy(hours = iosMapHours(details))
}

@Composable
private fun iosMapHours(details: MapShopDetails?): String? {
    val hours = details?.hours ?: return null
    if (hours.isClosed) return stringResource(Res.string.discovery_map_card_closed)
    val interval = hours.intervalLabel() ?: return null
    return stringResource(Res.string.discovery_map_card_hours,
        stringResource(if (details.isOpen) Res.string.discovery_map_card_open else Res.string.discovery_map_card_closed), interval)
}

private fun iosMapShops(state: MapState): List<ShopSummary> {
    val markers = (state.carouselShops + listOfNotNull(state.selectedShop).filterNot { selected -> state.carouselShops.any { it.id == selected.id } }).distinctBy { it.id }
    return markers.map { marker ->
        val details = if (state.selectedShop?.id == marker.id) state.details ?: state.cardDetails[marker.id]
            else state.cardDetails[marker.id]
        ShopSummary(marker.id, marker.title, details?.rating, details?.reviewCount ?: 0,
            photoUrl = details?.photoUrl, isOpen = details?.isOpen == true, kind = marker.kind, coordinates = marker.coordinates)
    }
}

@Composable
private fun iosLocationNotice(search: SearchState, map: MapState, expanded: Boolean): String? = when {
    search.nearbyRequested -> stringResource(Res.string.discovery_location_pending)
    search.locationUnavailable || search.locationSettingsNeeded || (expanded && map.locationSettingsNeeded) ->
        stringResource(Res.string.discovery_location_unavailable)
    else -> null
}

@Composable
private fun iosChips(state: SearchState): List<DiscoveryIosChip> = with(state.filters) {
    listOf(
        DiscoveryIosChip("nearby", stringResource(Res.string.discovery_quick_nearby), nearbyOnly),
        DiscoveryIosChip("open-only", stringResource(Res.string.discovery_quick_open), openOnly),
        DiscoveryIosChip("new", stringResource(Res.string.discovery_quick_new), newOnly),
        DiscoveryIosChip("visited", stringResource(Res.string.discovery_quick_visited), visitedOnly),
        DiscoveryIosChip("favorites", stringResource(Res.string.discovery_quick_favorites), favoritesOnly),
        DiscoveryIosChip(ShopKind.Specialty.name, stringResource(Res.string.discovery_kind_specialty), kind == ShopKind.Specialty),
        DiscoveryIosChip(ShopKind.CoffeeBar.name, stringResource(Res.string.discovery_kind_coffee_bar), kind == ShopKind.CoffeeBar),
        DiscoveryIosChip(ShopKind.Cafe.name, stringResource(Res.string.discovery_kind_cafe), kind == ShopKind.Cafe),
    )
}

internal fun discoveryIosCanvas(state: MapState, preview: Boolean) = MapCanvasState(
    state.markers, if (state.showZones && !preview) state.zones else emptyList(),
    state.selectedShop?.id, state.camera, state.myLocationRequest, preview,
    clusters = state.canvasClusters(preview), heatmap = state.canvasHeatmap(preview),
)

internal fun MapState.iosViewportCamera(): MapCamera? {
    val bounds = activeBounds ?: return null
    val zoom = activeZoom ?: return null
    return MapCamera(Coordinates((bounds.minLat + bounds.maxLat) / 2, (bounds.minLon + bounds.maxLon) / 2), zoom)
}
