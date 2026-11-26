package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.api.MapCamera
import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.domain.model.*
import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.domain.rules.visibleShops
import com.coffeepeek.feature.discovery.impl.map.*
import com.coffeepeek.feature.discovery.impl.search.*

internal fun iosShopRows(state: SearchState, picker: Boolean): List<ShopSummary> =
    if (picker) visibleShops(state.shops, state.pickerResultCriteria?.filters ?: state.filters).distinctBy { it.id }
    else state.visibleShops

internal fun iosRecentRows(state: SearchState, picker: Boolean): List<ShopSummary> {
    val applied = state.pickerResultCriteria
    val browsing = searchText(state.query) == null && !state.filters.nearbyOnly &&
        searchText(applied?.text.orEmpty()) == null && applied?.filters?.nearbyOnly != true
    return if (picker && browsing) state.pickerRecentShops.distinctBy { it.id } else emptyList()
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

internal fun SearchState.iosCanSuggestRoaster(hasRoasters: Boolean?): Boolean =
    query.isNotBlank() && hasRoasters == false && emptyReason != null
