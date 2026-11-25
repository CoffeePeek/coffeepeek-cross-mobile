package com.coffeepeek.feature.discovery.impl.ui

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
import com.coffeepeek.feature.discovery.impl.ui.compose.component.intervalLabel

internal suspend fun DiscoveryIosResources.discoveryIosState(snapshot: NativeDiscoverySnapshot, picker: Boolean): DiscoveryIosState {
    val search = snapshot.search
    val recents = iosRecentRows(search, picker)
    val recentIds = recents.map { it.id }.toSet()
    val resolved = ResolvedDiscoveryIosContent(
        iosShopRows(search, picker).filterNot { it.id in recentIds }.map { iosShop(it, search) },
        recents.map { iosShop(it, search) }, iosMapRows(snapshot.map, search),
        snapshot.map.suggestions.map { iosShop(it, search) }, iosChips(search, picker), discoveryIosLabels(picker),
        iosLocationNotice(search, snapshot.map, snapshot.overlay.mapExpanded))
    return snapshot.iosState(picker, resolved, search.checkInShopPickerResults())
}

private data class ResolvedDiscoveryIosContent(val shops: List<DiscoveryIosShop>, val recents: List<DiscoveryIosShop>,
    val mapShops: List<DiscoveryIosShop>, val suggestions: List<DiscoveryIosShop>, val chips: List<DiscoveryIosChip>,
    val labels: Map<String, String>, val locationNotice: String?)

internal fun nativeDiscoveryLoadingState(picker: Boolean): DiscoveryIosState {
    val empty = NativeDiscoverySnapshot(SearchState(), MapState(), NativeDiscoveryOverlay(), false)
    return empty.iosState(picker, ResolvedDiscoveryIosContent(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), null),
        CheckInShopPickerResults(null, false, true))
}

private fun NativeDiscoverySnapshot.iosState(picker: Boolean, resolved: ResolvedDiscoveryIosContent,
    pickerResults: CheckInShopPickerResults): DiscoveryIosState = DiscoveryIosState(
        picker, overlay.active && !search.showFilters, search.query, search.showsDiscovery, overlay.mapExpanded, overlay.roastersExpanded,
        !picker && search.query.isNotBlank() && overlay.hasRoasters != false,
        !picker && search.iosCanSuggestRoaster(overlay.hasRoasters), overlay.scrollRevision, overlay.bottomClearance,
        search.isLoading || (picker && (pickerResults.pending || search.isRefreshing)), search.isRefreshing, search.isLoadingMore,
        search.canLoadMore && (!picker || search.pickerResultCriteria?.matchesPickerQuery(search.criteria) == true),
        search.error != null, search.loadMoreError != null, !picker && search.canSuggestShop,
        search.emptyReason?.name, resolved.locationNotice,
        search.locationSettingsNeeded || (overlay.mapExpanded && map.locationSettingsNeeded), search.favoriteFailureId != null,
        overlay.canContinue && !overlay.mapExpanded && !overlay.roastersExpanded,
        resolved.shops, resolved.recents, resolved.mapShops, map.selectedShop?.id,
        resolved.suggestions, map.showSuggestions, map.searchFailed, map.isSearchLoading,
        map.selectedZone?.let { DiscoveryIosZone(it.name, it.description, it.shopCount) },
        map.showZones, map.showHeatmap, map.isHeatmapLoading, map.heatmapFailed,
        map.viewportFailed, map.isTruncated, resolved.chips, search.activeFilterCount, resolved.labels,
        pickerResults.query, pickerResults.nearby, !picker && search.hasNoSearchMatches(overlay.hasRoasters),
    )

private suspend fun DiscoveryIosResources.iosShop(shop: ShopSummary, state: SearchState): DiscoveryIosShop {
    val distance = shop.distanceMeters ?: state.userLocation?.let { user -> shop.coordinates?.let { distanceMeters(user, it) } }
    return DiscoveryIosShop(shop.id, shop.title, shop.photoUrl, shop.address,
        shop.rating?.takeIf { it > 0 }?.let(::oneDecimal),
        if (shop.checkInCount > 0) text(Res.string.discovery_map_card_checkins, shop.checkInCount) else null,
        distance?.let { this.distance(it) }, text(when (shop.kind) {
            ShopKind.Specialty -> Res.string.discovery_shop_kind_specialty
            ShopKind.CoffeeBar -> Res.string.discovery_shop_kind_coffee_bar
            ShopKind.Cafe -> Res.string.discovery_shop_kind_cafe
        }), shop.tags, shop.isFavorite, shop.id in state.favoriteUpdates,
        text(if (shop.isFavorite) Res.string.discovery_favorite_remove else Res.string.discovery_favorite_add),
        shop.isOpen, shop.isNew, null, DiscoveryIosShopDecoration(shop.priceLevel?.level ?: 0,
            shop.checkInCount, shop.brewMethods.map { DiscoveryIosBrewMethod(it, brew(it)) }, shop.roasterLogos))
}

private suspend fun DiscoveryIosResources.iosMapRows(map: MapState, search: SearchState): List<DiscoveryIosShop> = iosMapShops(map).map {
    val details = map.cardDetails[it.id] ?: map.details.takeIf { _ -> it.id == map.selectedShop?.id }
    iosShop(it, search).copy(hours = iosMapHours(details))
}

private suspend fun DiscoveryIosResources.iosMapHours(details: MapShopDetails?): String? {
    val hours = details?.hours ?: return null
    if (hours.isClosed) return text(Res.string.discovery_map_card_closed)
    val interval = hours.intervalLabel() ?: return null
    return text(Res.string.discovery_map_card_hours,
        text(if (details.isOpen) Res.string.discovery_map_card_open else Res.string.discovery_map_card_closed), interval)
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

private suspend fun DiscoveryIosResources.iosLocationNotice(search: SearchState, map: MapState, expanded: Boolean): String? = when {
    search.nearbyRequested -> text(Res.string.discovery_location_pending)
    search.locationUnavailable || search.locationSettingsNeeded || (expanded && map.locationSettingsNeeded) ->
        text(Res.string.discovery_location_unavailable)
    else -> null
}

private suspend fun DiscoveryIosResources.iosChips(state: SearchState, picker: Boolean): List<DiscoveryIosChip> = with(state.filters) {
    listOf(
        DiscoveryIosChip("nearby", text(Res.string.discovery_quick_nearby), nearbyOnly || (picker && state.nearbyRequested)),
        DiscoveryIosChip("open-only", text(Res.string.discovery_quick_open), openOnly),
        DiscoveryIosChip("new", text(Res.string.discovery_quick_new), newOnly),
        DiscoveryIosChip("visited", text(Res.string.discovery_quick_visited), visitedOnly),
        DiscoveryIosChip("favorites", text(Res.string.discovery_quick_favorites), favoritesOnly),
        DiscoveryIosChip(ShopKind.Specialty.name, text(Res.string.discovery_kind_specialty), kind == ShopKind.Specialty),
        DiscoveryIosChip(ShopKind.CoffeeBar.name, text(Res.string.discovery_kind_coffee_bar), kind == ShopKind.CoffeeBar),
        DiscoveryIosChip(ShopKind.Cafe.name, text(Res.string.discovery_kind_cafe), kind == ShopKind.Cafe),
    )
}
