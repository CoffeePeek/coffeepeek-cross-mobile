package com.coffeepeek.admin.di.discovery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.component.platformReduceMotion
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.domain.model.MapBounds as LegacyBounds
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
import org.jetbrains.compose.resources.painterResource

/** The legacy pieces Discovery borrows: the photo loader and its placeholders, the brew method icons, the map, the location permission, the analytics exposure. */
internal fun discoveryHost() = DiscoveryHost(
    image = { url, description, modifier, scale -> CoffeeShopImage(imageUrl = url, modifier = modifier, contentDescription = description, contentScale = scale) },
    coverPlaceholder = { description, modifier -> CoffeeShopPlaceholderImage(modifier, contentDescription = description ?: "Фото кофейни отсутствует") },
    brewMethodIcon = { name ->
        Icon(org.jetbrains.compose.resources.painterResource(brewMethodIcon(name)), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
    },
    roasterLogo = { _, logoUrl -> if (logoUrl != null) CoffeeShopImage(imageUrl = logoUrl, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, placeholderLabelSize = 5.sp) },
    suggestionIllustration = { painterResource(Res.drawable.maskot_with_magnifying_glass) },
    exposure = { modifier, exposure ->
        when (exposure) {
            DiscoveryExposure.Search -> modifier.analyticsExposure("search", "shops", "discovery")
            DiscoveryExposure.Filters -> modifier.analyticsExposure("filters", "shops", "discovery")
            DiscoveryExposure.ShopCatalog -> modifier.analyticsExposure("shop_catalog", "discovery", "discovery")
            DiscoveryExposure.Favorite -> modifier.favoriteExposure()
            DiscoveryExposure.MapPreview -> modifier.analyticsExposure("map", "home", "discovery")
            DiscoveryExposure.MapFull -> modifier.analyticsExposure("map", "discovery", "map")
        }
    },
    haptic = { platformHaptic(HapticEvent.Selection) },
    retained = { visible, content -> RetainedContent(visible, content) },
    locationPermission = { onGranted, onDenied -> LocationPermissionEffect(requestKey = 1, onGranted = onGranted, onDenied = onDenied) },
    photoPrefetch = { listState, photos -> PrefetchListPhotos(listState, photos) },
    reduceMotion = { platformReduceMotion() },
    mapCanvas = { state, callbacks, modifier -> DiscoveryMapCanvas(state, callbacks, modifier) },
)

@Composable
private fun DiscoveryMapCanvas(state: MapCanvasState, callbacks: MapCanvasCallbacks, modifier: Modifier) {
    val shops = androidx.compose.runtime.remember(state.markers) { state.markers.map { it.toLegacy() } }
    val zones = androidx.compose.runtime.remember(state.zones) { state.zones.map { it.toLegacy() } }
    CoffeeMap(
        shops = shops,
        zones = zones,
        selectedShopId = state.selectedShopId,
        onBoundsChanged = { bounds, zoom -> callbacks.onBoundsChanged(MapBounds(bounds.minLat, bounds.minLon, bounds.maxLat, bounds.maxLon), zoom) },
        onShopClick = { callbacks.onMarkerClick(it.id) },
        onZoneClick = { callbacks.onZoneClick(it.id) },
        modifier = modifier,
        cameraTarget = state.camera?.let { it.target.latitude to it.target.longitude },
        cameraZoom = state.camera?.zoom,
        onCameraTargetApplied = callbacks.onCameraApplied,
        isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
        myLocationRequestKey = state.myLocationRequest,
        onMyLocationFound = { latitude, longitude -> callbacks.onMyLocationFound(com.coffeepeek.feature.discovery.domain.model.Coordinates(latitude, longitude)) },
        onLocationPermissionDenied = callbacks.onLocationDenied,
        requestLocationPermissionOnLoad = !state.isPreview,
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
