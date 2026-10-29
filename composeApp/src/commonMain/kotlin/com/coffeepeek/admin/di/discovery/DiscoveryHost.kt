package com.coffeepeek.admin.di.discovery


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.analytics.LocalAnalyticsVisible
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.map.mapCanvasSize
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.component.platformReduceMotion
import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.MapCanvasCallbacks
import com.coffeepeek.feature.discovery.api.MapCanvasState
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.domain.model.ShopKind

/** Root supplies platform services and exposure ownership, never business visual slots. */
internal fun discoveryHost() = DiscoveryHost(
    exposure = ::discoveryExposure,
    haptic = { platformHaptic(HapticEvent.Selection) },
    retained = { visible, content -> RetainedContent(visible, content) },
    locationPermission = { requestKey, onGranted, onDenied ->
        LocationPermissionEffect(requestKey, onGranted, onDenied, requestOnce = true)
    },
    reduceMotion = { platformReduceMotion() },
    isVisible = { LocalAnalyticsVisible.current },
    mapCanvas = { state, callbacks, modifier -> DiscoveryMapCanvas(state, callbacks, modifier) },
)

private fun discoveryExposure(modifier: Modifier, exposure: DiscoveryExposure): Modifier = when (exposure) {
    DiscoveryExposure.Search -> modifier.analyticsExposure("search", "shops", "discovery")
    DiscoveryExposure.Filters -> modifier.analyticsExposure("filters", "shops", "discovery")
    DiscoveryExposure.ShopCatalog -> modifier.analyticsExposure("shop_catalog", "discovery", "discovery")
    DiscoveryExposure.Favorite -> modifier.favoriteExposure()
    DiscoveryExposure.MapPreview -> modifier.analyticsExposure("map", "home", "discovery")
    DiscoveryExposure.MapFull -> modifier.analyticsExposure("map", "discovery", "map")
}

@Composable
private fun DiscoveryMapCanvas(state: MapCanvasState, callbacks: MapCanvasCallbacks, modifier: Modifier) {
    val shops = androidx.compose.runtime.remember(state.markers) { state.markers.map { it.toLegacy() } }
    val zones = androidx.compose.runtime.remember(state.zones) { state.zones.map { it.toLegacy() } }
    CoffeeMap(
        shops = shops,
        clusters = state.clusters.map { cluster ->
            com.coffeepeek.domain.model.MapCluster(cluster.id, cluster.coordinates.latitude, cluster.coordinates.longitude, cluster.count,
                com.coffeepeek.domain.model.MapBounds(cluster.bounds.minLat, cluster.bounds.minLon, cluster.bounds.maxLat, cluster.bounds.maxLon))
        },
        zones = zones,
        selectedShopId = state.selectedShopId,
        onBoundsChanged = { bounds, zoom -> callbacks.onBoundsChanged(MapBounds(bounds.minLat, bounds.minLon, bounds.maxLat, bounds.maxLon), zoom) },
        onShopClick = { callbacks.onMarkerClick(it.id) },
        onZoneClick = { callbacks.onZoneClick(it.id) },
        modifier = modifier.mapCanvasSize(state.canvasSize),
        cameraTarget = state.camera?.let { it.target.latitude to it.target.longitude },
        cameraZoom = state.camera?.zoom,
        onCameraTargetApplied = callbacks.onCameraApplied,
        isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
        myLocationRequestKey = state.myLocationRequest,
        onMyLocationFound = { latitude, longitude -> callbacks.onMyLocationFound(com.coffeepeek.feature.discovery.domain.model.Coordinates(latitude, longitude)) },
        onLocationPermissionDenied = callbacks.onLocationDenied,
        requestLocationPermissionOnLoad = false,
        showAttribution = !state.isPreview,
    )
}

private fun MapMarker.toLegacy() = MapShop(
    id = id, title = title, latitude = coordinates.latitude, longitude = coordinates.longitude, primaryZoneId = zoneId,
    type = when (kind) { ShopKind.Specialty -> CoffeeShopType.SPECIALTY; ShopKind.CoffeeBar -> CoffeeShopType.COFFEE_BAR; ShopKind.Cafe -> CoffeeShopType.CAFE },
)

private fun MapZone.toLegacy() = MapCoffeeZone(
    id = id, name = name, description = description, color = color, latitude = center.latitude, longitude = center.longitude,
    radiusMeters = radiusMeters, shopCount = shopCount, polygon = polygon.map { it.latitude to it.longitude },
)

/** The favorite heart's exposure names the page it is on, as the legacy favorite button did. */
private fun Modifier.favoriteExposure(): Modifier = composed {
    val page = com.coffeepeek.admin.analytics.Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    analyticsExposure("favorites", when (page) { "shop_detail" -> "shop_detail"; "roaster_detail" -> "roaster_card"; else -> "catalog_card" }, page)
}
