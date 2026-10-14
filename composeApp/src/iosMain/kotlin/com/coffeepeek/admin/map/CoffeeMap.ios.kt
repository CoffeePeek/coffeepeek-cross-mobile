@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.coffeepeek.admin.map

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import kotlinx.coroutines.launch

private const val TARGET_ZOOM = 16f
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
    requestLocationPermissionOnLoad: Boolean,
    showAttribution: Boolean,
) {
    val boundsCallback = rememberUpdatedState(onBoundsChanged)
    val shopCallback = rememberUpdatedState(onShopClick)
    val targetAppliedCallback = rememberUpdatedState(onCameraTargetApplied)
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
        requestLocationPermissionOnLoad = requestLocationPermissionOnLoad,
        showAttribution = showAttribution,
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
    requestLocationPermissionOnLoad: Boolean,
    showAttribution: Boolean,
) {
    val boundsCallback = rememberUpdatedState(onBoundsChanged)
    val shopCallback = rememberUpdatedState(onShopClick)
    val zoneCallback = rememberUpdatedState(onZoneClick)
    val locationFoundCallback = rememberUpdatedState(onMyLocationFound)
    val locationDeniedCallback = rememberUpdatedState(onLocationPermissionDenied)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val initiallyHasTarget = remember { cameraTarget != null }
    var hasLocationPermission by remember { mutableStateOf(PlatformLocation.hasPermission()) }
    var handledLocationRequestKey by remember { mutableStateOf<Int?>(null) }
    val hasCameraTarget = rememberUpdatedState(cameraTarget != null)
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
            session.moveCamera(target.first, target.second, cameraZoom ?: TARGET_ZOOM, animated = true)
            onCameraTargetApplied()
        }
    }

    fun findAndShowLocation() {
        hasLocationPermission = true
        val initial = myLocationRequestKey == 0
        if ((initial && initiallyHasTarget) || handledLocationRequestKey == myLocationRequestKey) return
        handledLocationRequestKey = myLocationRequestKey
        val cameraRevision = session.cameraRevision
        scope.launch {
            val point = PlatformLocation.getLastKnownLocation()
            if (point == null) {
                if (!initial) locationDeniedCallback.value()
            } else if (!initial || (!hasCameraTarget.value && session.cameraRevision == cameraRevision)) {
                session.moveCamera(point.latitude, point.longitude, LOCATION_ZOOM, animated = true)
                locationFoundCallback.value(point.latitude, point.longitude)
            }
        }
    }

    LaunchedEffect(session) {
        if (PlatformLocation.hasPermission()) findAndShowLocation()
    }

    val showsUserLocation = hasLocationPermission
    Box(modifier = modifier) {
        val locationRequestedByMap = requestLocationPermissionOnLoad || myLocationRequestKey > 0
        // Keep the preview's location puck in sync when another screen grants permission.
        // A zero request key observes authorization without presenting a prompt.
        LocationPermissionEffect(
            requestKey = if (requestLocationPermissionOnLoad) myLocationRequestKey + 1 else myLocationRequestKey,
            onGranted = {
                if (locationRequestedByMap) findAndShowLocation() else hasLocationPermission = true
            },
            onDenied = {
                hasLocationPermission = false
                if (locationRequestedByMap) locationDeniedCallback.value()
            },
        )
        UIKitView(
            factory = { provider.createMapView().also(session::attach) },
            modifier = Modifier.matchParentSize(),
            // The map fills its rectangle; keep it below Compose markers, controls and sheets.
            properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative, placedAsOverlay = false),
            update = { mapView ->
                mapView.backgroundColor = colors.background.toUIColor()
                mapView.overrideUserInterfaceStyle = colors.uiKitStyle
                session.attach(mapView)
                session.updateContent(
                    shops, clusters, zones, selectedShopId, isDarkTheme, showAttribution, showsUserLocation,
                )
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
    private var pendingCamera: (() -> Unit)? = null
    var cameraRevision: Int = 0
        private set

    fun attach(view: platform.UIKit.UIView) {
        mapView = view
        pendingCamera?.also { pendingCamera = null }?.invoke()
    }

    fun detach() {
        mapView = null
        pendingCamera = null
    }

    fun updateContent(
        shops: List<MapShop>,
        clusters: List<MapCluster>,
        zones: List<MapCoffeeZone>,
        selectedShopId: String?,
        isDarkTheme: Boolean,
        showAttribution: Boolean,
        showsUserLocation: Boolean,
    ) {
        mapView?.let { view ->
            provider.updateMapView(
                mapView = view,
                stateJson = nativeMapStateJson(shops, clusters, zones, selectedShopId, isDarkTheme, showAttribution),
                showsUserLocation = showsUserLocation,
                callbacks = this,
            )
        }
    }

    fun moveCamera(latitude: Double, longitude: Double, zoom: Float, animated: Boolean) {
        cameraRevision++
        val move = { mapView?.let { view -> provider.moveCamera(view, latitude, longitude, zoom, animated) }; Unit }
        if (mapView == null) pendingCamera = move else move()
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
