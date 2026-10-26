package com.coffeepeek.feature.discovery.impl.map

import com.coffeepeek.feature.discovery.api.MapCamera
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapCluster
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.domain.model.ShopSummary

internal data class MapState(
    val cityId: String? = null,
    /** The ten nearest shops inside five kilometers of [nearbyOrigin]; the carousel shows them while nothing else is selected. */
    val nearby: List<MapMarker> = emptyList(),
    val nearbyOrigin: Coordinates? = null,
    val markers: List<MapMarker> = emptyList(),
    val clusters: List<MapCluster> = emptyList(),
    val zones: List<MapZone> = emptyList(),
    val zoneGroups: List<MapZone> = emptyList(),
    val showZones: Boolean = true,
    val selectedShop: MapMarker? = null,
    val selectedZone: MapZone? = null,
    val details: MapShopDetails? = null,
    val isLoadingDetails: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "",
    val suggestions: List<ShopSummary> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchFailed: Boolean = false,
    val showSuggestions: Boolean = false,
    /** The area could not be loaded: «Искать в этой области» offers a retry. */
    val showSearchArea: Boolean = false,
    val activeBounds: MapBounds? = null,
    val pendingBounds: MapBounds? = null,
    val activeZoom: Float? = null,
    val pendingZoom: Float? = null,
    val isTruncated: Boolean = false,
    val myLocationRequest: Int = 0,
    val camera: MapCamera? = null,
) {
    val hasCarousel: Boolean get() = selectedShop != null || nearby.isNotEmpty()
}

internal sealed interface MapAction {
    data class BoundsChanged(val bounds: MapBounds, val zoom: Float) : MapAction
    data object SearchArea : MapAction
    data class SelectMarker(val shopId: String) : MapAction
    data class SelectFromCarousel(val shopId: String) : MapAction
    data class SelectZone(val zoneId: String) : MapAction
    data object ShowZoneShops : MapAction
    data object ClearZone : MapAction
    data object ToggleZones : MapAction
    data class Query(val text: String) : MapAction
    data class SelectSuggestion(val shopId: String) : MapAction
    data object RequestMyLocation : MapAction
    data class MyLocationFound(val point: Coordinates) : MapAction
    data object LocationDenied : MapAction
    data object ZoomIn : MapAction
    data object ZoomOut : MapAction
    data class Focus(val focus: MapFocus) : MapAction
    data object CameraApplied : MapAction
    data class OpenShop(val shopId: String) : MapAction
}

internal sealed interface MapEvent {
    data class OpenShop(val shopId: String) : MapEvent
}
