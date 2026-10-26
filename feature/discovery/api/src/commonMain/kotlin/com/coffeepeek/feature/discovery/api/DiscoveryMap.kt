package com.coffeepeek.feature.discovery.api

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapCluster
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone

/** What the card of the selected shop shows besides the marker; the Shop owner backs it and the application formats the hours. */
data class MapShopDetails(val photoUrl: String?, val rating: Double?, val reviewCount: Int, val isOpen: Boolean, val hours: String?, val coordinates: Coordinates? = null)

interface DiscoveryShopDetails {
    suspend fun details(shopId: String): Result<MapShopDetails>
}

/** A request to bring a shop on the map into view and select it (from a shop page or a list). */
data class MapFocus(val shopId: String, val title: String, val coordinates: Coordinates)

data class MapCamera(val target: Coordinates, val zoom: Float)

/** What the map canvas (the application's map: MapLibre, the native iOS map) draws and obeys. */
data class MapCanvasState(
    val markers: List<MapMarker>,
    val zones: List<MapZone>,
    val selectedShopId: String?,
    /** A camera move to apply once; the canvas answers with [MapCanvasCallbacks.onCameraApplied]. */
    val camera: MapCamera?,
    /** Grows when the user asks for their position; the canvas finds it and answers with [MapCanvasCallbacks.onMyLocationFound]. */
    val myLocationRequest: Int,
    /** The small map of the tab: no attribution, no automatic permission request, no zones, taps open the shop. */
    val isPreview: Boolean = false,
    val clusters: List<MapCluster> = emptyList(),
)

class MapCanvasCallbacks(
    val onBoundsChanged: (MapBounds, Float) -> Unit,
    val onMarkerClick: (shopId: String) -> Unit,
    val onZoneClick: (zoneId: String) -> Unit,
    val onCameraApplied: () -> Unit,
    val onMyLocationFound: (Coordinates) -> Unit,
    val onLocationDenied: () -> Unit,
)
