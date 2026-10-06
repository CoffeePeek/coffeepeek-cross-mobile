@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.coffeepeek.admin.map

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import kotlinx.coroutines.launch

private const val DEFAULT_ZOOM = 12f
private const val LOCATION_ZOOM = 15f

@Composable
actual fun CoffeeMap(
    shops: List<MapShop>,
    clusters: List<MapCluster>,
    zones: List<MapCoffeeZone>,
    selectedShopId: String?,
    onBoundsChanged: (MapBounds, Float) -> Unit,
    onShopClick: (MapShop) -> Unit,
    onZoneClick: (MapCoffeeZone) -> Unit,
    modifier: Modifier,
    cameraTarget: Pair<Double, Double>?,
    cameraZoom: Float?,
    onCameraTargetApplied: () -> Unit,
    isDarkTheme: Boolean,
    myLocationRequestKey: Int,
    onMyLocationFound: (Double, Double) -> Unit,
    onLocationPermissionDenied: () -> Unit,
) {
    val boundsCallback = rememberUpdatedState(onBoundsChanged)
    val shopCallback = rememberUpdatedState(onShopClick)
    val targetAppliedCallback = rememberUpdatedState(onCameraTargetApplied)
    val locationFoundCallback = rememberUpdatedState(onMyLocationFound)
    val locationDeniedCallback = rememberUpdatedState(onLocationPermissionDenied)
    val zoneCallback = rememberUpdatedState(onZoneClick)
    val nativeProvider = checkNotNull(IosNativeMapRegistry.provider) {
        "MapLibre provider must be registered before CoffeeMap is displayed"
    }
    NativeMapContent(
        provider = nativeProvider,
        shops = shops,
        clusters = clusters,
        zones = zones,
        selectedShopId = selectedShopId,
        onBoundsChanged = { boundsCallback.value(it.first, it.second) },
        onShopClick = { shopId -> shops.firstOrNull { it.id == shopId }?.let(shopCallback.value) },
        onZoneClick = { zoneId -> zones.firstOrNull { it.id == zoneId }?.let(zoneCallback.value) },
        modifier = modifier,
        cameraTarget = cameraTarget,
        cameraZoom = cameraZoom,
        onCameraTargetApplied = { targetAppliedCallback.value() },
        isDarkTheme = isDarkTheme,
        myLocationRequestKey = myLocationRequestKey,
        onMyLocationFound = onMyLocationFound,
        onLocationPermissionDenied = onLocationPermissionDenied,
    )
}

@Composable
private fun NativeMapContent(
    provider: IosNativeMapProvider,
    shops: List<MapShop>,
    clusters: List<MapCluster>,
    zones: List<MapCoffeeZone>,
    selectedShopId: String?,
    onBoundsChanged: (Pair<MapBounds, Float>) -> Unit,
    onShopClick: (String) -> Unit,
    onZoneClick: (String) -> Unit,
    modifier: Modifier,
    cameraTarget: Pair<Double, Double>?,
    cameraZoom: Float?,
    onCameraTargetApplied: () -> Unit,
    isDarkTheme: Boolean,
    myLocationRequestKey: Int,
    onMyLocationFound: (Double, Double) -> Unit,
    onLocationPermissionDenied: () -> Unit,
) {
    val boundsCallback = rememberUpdatedState(onBoundsChanged)
    val shopCallback = rememberUpdatedState(onShopClick)
    val zoneCallback = rememberUpdatedState(onZoneClick)
    val locationFoundCallback = rememberUpdatedState(onMyLocationFound)
    val locationDeniedCallback = rememberUpdatedState(onLocationPermissionDenied)
    val scope = rememberCoroutineScope()
    val session = remember {
        NativeMapSession(
            provider = provider,
            onBoundsChanged = { bounds, zoom -> boundsCallback.value(bounds to zoom) },
            onShopClick = { shopCallback.value(it) },
            onZoneClick = { zoneCallback.value(it) },
        )
    }

    DisposableEffect(session) {
        onDispose { session.detach() }
    }

    LaunchedEffect(cameraTarget, cameraZoom) {
        cameraTarget?.let { target ->
            session.moveCamera(target.first, target.second, cameraZoom ?: DEFAULT_ZOOM, animated = true)
            onCameraTargetApplied()
        }
    }

    fun findAndShowLocation() {
        scope.launch {
            val point = PlatformLocation.getLastKnownLocation()
            if (point == null) {
                locationDeniedCallback.value()
            } else {
                session.moveCamera(point.latitude, point.longitude, LOCATION_ZOOM, animated = true)
                locationFoundCallback.value(point.latitude, point.longitude)
            }
        }
    }

    Box(modifier = modifier) {
        LocationPermissionEffect(
            requestKey = myLocationRequestKey,
            onGranted = ::findAndShowLocation,
            onDenied = { locationDeniedCallback.value() },
        )
        UIKitView(
            factory = { provider.createMapView().also(session::attach) },
            modifier = Modifier.matchParentSize(),
            update = { mapView ->
                session.attach(mapView)
                session.updateContent(shops, clusters, zones, selectedShopId, isDarkTheme)
            },
        )
    }
}

private class NativeMapSession(
    private val provider: IosNativeMapProvider,
    private val onBoundsChanged: (MapBounds, Float) -> Unit,
    private val onShopClick: (String) -> Unit,
    private val onZoneClick: (String) -> Unit,
) : IosNativeMapCallbacks {
    private var mapView: platform.UIKit.UIView? = null

    fun attach(view: platform.UIKit.UIView) {
        mapView = view
    }

    fun detach() {
        mapView = null
    }

    fun updateContent(
        shops: List<MapShop>,
        clusters: List<MapCluster>,
        zones: List<MapCoffeeZone>,
        selectedShopId: String?,
        isDarkTheme: Boolean,
    ) {
        mapView?.let { view ->
            provider.updateMapView(
                mapView = view,
                stateJson = nativeMapStateJson(shops, clusters, zones, selectedShopId, isDarkTheme),
                callbacks = this,
            )
        }
    }

    fun moveCamera(latitude: Double, longitude: Double, zoom: Float, animated: Boolean) {
        mapView?.let { view -> provider.moveCamera(view, latitude, longitude, zoom, animated) }
    }

    override fun onShopClick(shopId: String) = onShopClick.invoke(shopId)

    override fun onZoneClick(zoneId: String) = onZoneClick.invoke(zoneId)

    override fun onBoundsChanged(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        zoom: Float,
    ) = onBoundsChanged(MapBounds(minLat, minLon, maxLat, maxLon), zoom)
}

private fun nativeMapStateJson(
    shops: List<MapShop>,
    clusters: List<MapCluster>,
    zones: List<MapCoffeeZone>,
    selectedShopId: String?,
    isDarkTheme: Boolean,
): String = buildString {
    append('{')
    append("\"selectedShopId\":").append(selectedShopId.jsonValue()).append(',')
    append("\"dark\":").append(isDarkTheme).append(',')
    append("\"shops\":[")
    shops.joinTo(this, separator = ",") { shop ->
        "{\"id\":${shop.id.jsonValue()},\"title\":${shop.title.jsonValue()}," +
            "\"lat\":${shop.latitude},\"lon\":${shop.longitude},\"type\":${shop.type.jsonValue()}," +
            "\"selected\":${shop.id == selectedShopId}}"
    }
    append("],\"clusters\":[")
    clusters.joinTo(this, separator = ",") { cluster ->
        "{\"id\":${cluster.id.jsonValue()},\"lat\":${cluster.latitude}," +
            "\"lon\":${cluster.longitude},\"count\":${cluster.count}}"
    }
    append("],\"zones\":[")
    zones.joinTo(this, separator = ",") { zone ->
        "{\"id\":${zone.id.jsonValue()},\"name\":${zone.name.jsonValue()}," +
            "\"lat\":${zone.latitude},\"lon\":${zone.longitude}," +
            "\"radius\":${zone.radiusMeters},\"polygon\":[" +
            zone.polygon.joinToString(",") { (lat, lon) -> "[$lat,$lon]" } + "]}"
    }
    append("]}")
}

private fun String?.jsonValue(): String = if (this == null) {
    "null"
} else {
    buildString {
        append('"')
        for (character in this@jsonValue) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
