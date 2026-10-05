@file:Suppress("DEPRECATION")

package com.coffeepeek.admin.map

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.view.animation.LinearInterpolator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circlePitchAlignment
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.SymbolLayer
import kotlin.math.cos
import kotlin.math.sin

private const val DEFAULT_LAT = 53.9045
private const val DEFAULT_LON = 27.5615
private const val DEFAULT_ZOOM = 12f
private const val LOCATION_ZOOM = 15f
private const val TARGET_ZOOM = 16f
private const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
private const val OPEN_FREE_MAP_LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val OPEN_FREE_MAP_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

private data class ShopMark(
    var marker: Marker,
    var isSelected: Boolean,
    var latitude: Double,
    var longitude: Double,
    var shop: MapShop,
    var type: String,
)

private data class ClusterMark(
    var marker: Marker,
    var latitude: Double,
    var longitude: Double,
    var count: Int,
    var bounds: MapBounds,
)

private const val ZONE_SOURCE = "cp-zones"
private const val ZONE_LABEL_SOURCE = "cp-zone-labels"
private const val PULSE_SOURCE = "cp-selected-pulse"
private const val ZONE_FILL_LAYER = "cp-zones-fill"
private const val ZONE_LINE_LAYER = "cp-zones-line"
private const val ZONE_LABEL_LAYER = "cp-zone-labels"
private const val ZONE_LABEL_MAX_ZOOM = 15f
private const val PULSE_LAYER = "cp-selected-pulse"
private const val PROP_ZONE_ID = "zoneId"
private const val PROP_ZONE_COLOR = "zoneColor"
private const val PROP_ICON = "icon"
private const val PULSE_DURATION_MS = 1600L
private const val PULSE_MIN_RADIUS = 20f
private const val PULSE_MAX_RADIUS = 38f

/** Smooth continuous pulse: animates circle-layer paint props every frame instead of swapping bitmaps. */
private class SelectionPulse {
    var animator: ValueAnimator? = null
    var shopId: String? = null

    fun cancel() {
        animator?.cancel()
        animator = null
        shopId = null
    }
}

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
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val onBoundsChangedState = rememberUpdatedState(onBoundsChanged)
    val onShopClickState = rememberUpdatedState(onShopClick)
    val onZoneClickState = rememberUpdatedState(onZoneClick)
    val onCameraTargetAppliedState = rememberUpdatedState(onCameraTargetApplied)
    val onMyLocationFoundState = rememberUpdatedState(onMyLocationFound)
    val onLocationPermissionDeniedState = rememberUpdatedState(onLocationPermissionDenied)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.none { it }) {
            onLocationPermissionDeniedState.value()
        }
    }

    LaunchedEffect(requestLocationPermissionOnLoad) {
        if (requestLocationPermissionOnLoad && !context.hasLocationPermission()) {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    val mapView = remember {
        MapLibre.getInstance(appContext)
        MapView(appContext).apply { onCreate(null) }
    }
    val shopMarks = remember { mutableMapOf<String, ShopMark>() }
    val clusterMarks = remember { mutableMapOf<String, ClusterMark>() }
    val zonesState = rememberUpdatedState(zones)
    val animations = remember { SelectionPulse() }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleGeneration by remember { mutableIntStateOf(0) }
    var initialCameraApplied by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        var started = false
        var resumed = false

        fun start() {
            if (!started) {
                mapView.onStart()
                started = true
            }
        }

        fun resume() {
            start()
            if (!resumed) {
                mapView.onResume()
                resumed = true
            }
        }

        fun pause() {
            if (resumed) {
                mapView.onPause()
                resumed = false
            }
        }

        fun stop() {
            pause()
            if (started) {
                mapView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> pause()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        when {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> resume()
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> start()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            animations.cancel()
            stop()
        }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDestroy() }
    }

    DisposableEffect(map) {
        val activeMap = map ?: return@DisposableEffect onDispose { }
        val idleListener = MapLibreMap.OnCameraIdleListener {
            onBoundsChangedState.value(
                activeMap.projection.visibleRegion.latLngBounds.toMapBounds(),
                activeMap.cameraPosition.zoom.toFloat(),
            )
        }

        activeMap.addOnCameraIdleListener(idleListener)
        activeMap.setOnMarkerClickListener { marker ->
            val shop = shopMarks.values.firstOrNull { it.marker.id == marker.id }
            if (shop != null) {
                onShopClickState.value(shop.shop)
                true
            } else {
                val cluster = clusterMarks.values.firstOrNull { it.marker.id == marker.id }
                if (cluster != null) {
                    zoomToBounds(activeMap, mapView.width, mapView.height, cluster.bounds)
                    true
                } else {
                    false
                }
            }
        }
        // Zones are style layers under the markers; marker taps are consumed first, so shops win.
        val zoneClickListener = MapLibreMap.OnMapClickListener { point ->
            val screen = activeMap.projection.toScreenLocation(point)
            val zoneId = activeMap.queryRenderedFeatures(screen, ZONE_LABEL_LAYER, ZONE_FILL_LAYER)
                .firstNotNullOfOrNull { it.getStringProperty(PROP_ZONE_ID) }
            val zone = zoneId?.let { id -> zonesState.value.firstOrNull { it.id == id } }
            if (zone != null) onZoneClickState.value(zone)
            zone != null
        }
        activeMap.addOnMapClickListener(zoneClickListener)

        onDispose {
            activeMap.removeOnCameraIdleListener(idleListener)
            activeMap.removeOnMapClickListener(zoneClickListener)
            activeMap.setOnMarkerClickListener(null)
        }
    }

    LaunchedEffect(map, isDarkTheme) {
        val activeMap = map ?: return@LaunchedEffect
        animations.cancel()
        activeMap.removeAnnotations()
        shopMarks.clear()
        clusterMarks.clear()
        currentLocationMarker = null
        activeMap.setStyle(Style.Builder().fromUri(coffeeMapStyleUri(isDarkTheme))) { style ->
            styleGeneration += 1
            applyCoffeePeekMapStyle(style, isDarkTheme)
            addOverlayLayers(style, isDarkTheme)
            if (!initialCameraApplied) {
                val location = context.lastKnownLocation()
                currentLocation = location?.let { LatLng(it.latitude, it.longitude) }
                val initialTarget = cameraTarget?.let { LatLng(it.first, it.second) }
                    ?: currentLocation
                    ?: LatLng(DEFAULT_LAT, DEFAULT_LON)
                val initialZoom = cameraZoom ?: when {
                    cameraTarget != null -> TARGET_ZOOM
                    location != null -> LOCATION_ZOOM
                    else -> DEFAULT_ZOOM
                }
                activeMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialTarget, initialZoom.toDouble()))
                initialCameraApplied = true
                // Don't wait for a camera-idle event: report the first viewport right away so the
                // map loads shops on open instead of after the user's first gesture.
                onBoundsChangedState.value(
                    activeMap.projection.visibleRegion.latLngBounds.toMapBounds(),
                    activeMap.cameraPosition.zoom.toFloat(),
                )
                location?.takeIf { cameraTarget == null }?.let {
                    onMyLocationFoundState.value(it.latitude, it.longitude)
                }
            }
        }
    }

    LaunchedEffect(cameraTarget, cameraZoom, map, styleGeneration) {
        val target = cameraTarget ?: return@LaunchedEffect
        val activeMap = map ?: return@LaunchedEffect
        if (styleGeneration == 0) return@LaunchedEffect
        activeMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.first, target.second),
                (cameraZoom ?: TARGET_ZOOM).toDouble(),
            ),
            450,
        )
        onCameraTargetAppliedState.value()
    }

    LaunchedEffect(myLocationRequestKey, map, styleGeneration) {
        if (myLocationRequestKey == 0 || styleGeneration == 0) return@LaunchedEffect
        val activeMap = map ?: return@LaunchedEffect
        if (!context.hasLocationPermission()) {
            locationPermissionLauncher.launch(locationPermissions)
            return@LaunchedEffect
        }

        context.lastKnownLocation()?.let { location ->
            currentLocation = LatLng(location.latitude, location.longitude)
            activeMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    LOCATION_ZOOM.toDouble(),
                ),
                450,
            )
            onMyLocationFoundState.value(location.latitude, location.longitude)
        }
    }

    LaunchedEffect(currentLocation, map, styleGeneration) {
        val activeMap = map ?: return@LaunchedEffect
        val position = currentLocation ?: return@LaunchedEffect
        if (styleGeneration == 0) return@LaunchedEffect

        val marker = currentLocationMarker
        if (marker == null) {
            currentLocationMarker = activeMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(
                        IconFactory.getInstance(appContext)
                            .fromBitmap(MapMarkerIcons.myLocationBitmap(appContext)),
                    ),
            )
        } else {
            marker.position = position
            activeMap.updateMarker(marker)
        }
    }

    LaunchedEffect(shops, clusters, zones, selectedShopId, isDarkTheme, styleGeneration, map) {
        val activeMap = map ?: return@LaunchedEffect
        if (styleGeneration == 0) return@LaunchedEffect
        syncMapMarkers(
            context = appContext,
            map = activeMap,
            shops = shops,
            clusters = clusters,
            zones = zones,
            selectedShopId = selectedShopId,
            isDarkTheme = isDarkTheme,
            shopMarks = shopMarks,
            clusterMarks = clusterMarks,
            animations = animations,
            reduceMotion = appContext.prefersReducedMotion(),
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
                mapView.apply {
                    getMapAsync { readyMap ->
                        readyMap.uiSettings.apply {
                            isLogoEnabled = false
                            isAttributionEnabled = false
                            isCompassEnabled = false
                        }
                        readyMap.setPrefetchesTiles(true)
                        map = readyMap
                    }
                }
            },
        )
        Text(
            text = "OpenFreeMap · OpenMapTiles · © OSM",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(4.dp),
                )
                .clickable { uriHandler.openUri(OSM_COPYRIGHT_URL) }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
        )
    }
}

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.lastKnownLocation(): Location? {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    if (!hasLocationPermission()) return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider ->
            try { locationManager.getLastKnownLocation(provider) }
                catch (_: SecurityException) { null }
                catch (_: IllegalArgumentException) { null }
        }
        .maxByOrNull { it.time }
}

private fun Context.prefersReducedMotion(): Boolean {
    val duration = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transition = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return duration == 0f || transition == 0f
}

private fun syncMapMarkers(
    context: Context,
    map: MapLibreMap,
    shops: List<MapShop>,
    clusters: List<MapCluster>,
    zones: List<MapCoffeeZone>,
    selectedShopId: String?,
    isDarkTheme: Boolean,
    shopMarks: MutableMap<String, ShopMark>,
    clusterMarks: MutableMap<String, ClusterMark>,
    animations: SelectionPulse,
    reduceMotion: Boolean,
) {
    val selected = shops.firstOrNull { it.id == selectedShopId }
    val shopIds = shops.map { it.id }.toSet()
    shopMarks.keys.filter { it !in shopIds }.toList().forEach { id ->
        shopMarks.remove(id)?.let { map.removeMarker(it.marker) }
    }
    val clusterKeys = clusters.map { it.id }.toSet()
    clusterMarks.keys.filter { it !in clusterKeys }.toList().forEach { key ->
        clusterMarks.remove(key)?.let { map.removeMarker(it.marker) }
    }

    clusters.forEach { cluster ->
        val position = LatLng(cluster.latitude, cluster.longitude)
        val existing = clusterMarks[cluster.id]
        if (existing == null) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(IconFactory.getInstance(context).fromBitmap(MapMarkerIcons.clusterBitmap(context, cluster.count))),
            )
            clusterMarks[cluster.id] = ClusterMark(
                marker = marker,
                latitude = cluster.latitude,
                longitude = cluster.longitude,
                count = cluster.count,
                bounds = cluster.bounds,
            )
        } else {
            existing.bounds = cluster.bounds
            if (existing.latitude != cluster.latitude || existing.longitude != cluster.longitude) {
                existing.marker.position = position
                existing.latitude = cluster.latitude
                existing.longitude = cluster.longitude
            }
            if (existing.count != cluster.count) {
                existing.marker.setIcon(
                    IconFactory.getInstance(context).fromBitmap(MapMarkerIcons.clusterBitmap(context, cluster.count)),
                )
                existing.count = cluster.count
            }
            map.updateMarker(existing.marker)
        }
    }

    map.style?.let { style -> syncZoneLayers(context, style, zones, isDarkTheme) }

    shops.forEach { shop ->
        val position = LatLng(shop.latitude, shop.longitude)
        val isSelected = shop.id == selectedShopId
        val visual = if (isSelected) MapPinVisual.Selected else MapPinVisual.Default
        val existing = shopMarks[shop.id]
        if (existing == null) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(IconFactory.getInstance(context).fromBitmap(MapMarkerIcons.pinBitmap(context, shop.type, visual))),
            )
            val entry = ShopMark(
                marker = marker,
                isSelected = isSelected,
                latitude = shop.latitude,
                longitude = shop.longitude,
                shop = shop,
                type = shop.type,
            )
            shopMarks[shop.id] = entry
        } else {
            existing.shop = shop
            if (existing.latitude != shop.latitude || existing.longitude != shop.longitude) {
                existing.marker.position = position
                existing.latitude = shop.latitude
                existing.longitude = shop.longitude
            }
            if (existing.isSelected != isSelected || existing.type != shop.type) {
                existing.marker.setIcon(
                    IconFactory.getInstance(context).fromBitmap(MapMarkerIcons.pinBitmap(context, shop.type, visual)),
                )
                existing.isSelected = isSelected
                existing.type = shop.type
            }
            map.updateMarker(existing.marker)
        }
    }

    syncSelectionPulse(map, selected, animations, reduceMotion)
}

private fun syncSelectionPulse(
    map: MapLibreMap,
    selected: MapShop?,
    pulse: SelectionPulse,
    reduceMotion: Boolean,
) {
    val style = map.style ?: return
    val source = style.getSourceAs<GeoJsonSource>(PULSE_SOURCE) ?: return
    if (selected == null) {
        pulse.cancel()
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    source.setGeoJson(Point.fromLngLat(selected.longitude, selected.latitude))
    if (pulse.shopId == selected.id) return

    pulse.cancel()
    pulse.shopId = selected.id
    if (reduceMotion) {
        // Static soft halo instead of motion.
        style.getLayerAs<CircleLayer>(PULSE_LAYER)?.setProperties(
            circleRadius(PULSE_MIN_RADIUS + 6f),
            circleOpacity(0.18f),
            circleStrokeOpacity(0.35f),
        )
        return
    }
    pulse.animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = PULSE_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            val t = animator.animatedValue as Float
            val eased = 1f - (1f - t) * (1f - t) * (1f - t) // ease-out cubic: quick start, gentle settle
            val fade = (1f - t) * (1f - t)
            map.style?.getLayerAs<CircleLayer>(PULSE_LAYER)?.setProperties(
                circleRadius(PULSE_MIN_RADIUS + (PULSE_MAX_RADIUS - PULSE_MIN_RADIUS) * eased),
                circleOpacity(0.22f * fade),
                circleStrokeOpacity(0.6f * fade),
            )
        }
        start()
    }
}

private data class ZonePalette(val fillOpacity: Float, val lineOpacity: Float)

private fun zonePalette(isDarkTheme: Boolean) = if (isDarkTheme) {
    ZonePalette(fillOpacity = 0.16f, lineOpacity = 0.75f)
} else {
    ZonePalette(fillOpacity = 0.18f, lineOpacity = 0.80f)
}

/** Zone + pulse layers, inserted below MapLibre's marker layer so shop pins always draw on top. */
private fun addOverlayLayers(style: Style, isDarkTheme: Boolean) {
    val palette = zonePalette(isDarkTheme)
    listOf(ZONE_SOURCE, ZONE_LABEL_SOURCE, PULSE_SOURCE).forEach { id ->
        if (style.getSource(id) == null) style.addSource(GeoJsonSource(id))
    }
    val markerLayerId = style.layers.firstOrNull { it.id.startsWith("org.maplibre.annotations") }?.id
    fun add(layer: Layer) {
        if (style.getLayer(layer.id) != null) return
        if (markerLayerId != null) style.addLayerBelow(layer, markerLayerId) else style.addLayer(layer)
    }
    add(
        FillLayer(ZONE_FILL_LAYER, ZONE_SOURCE).withProperties(
            fillColor(get(PROP_ZONE_COLOR)),
            fillOpacity(palette.fillOpacity),
        ),
    )
    add(
        LineLayer(ZONE_LINE_LAYER, ZONE_SOURCE).withProperties(
            lineColor(get(PROP_ZONE_COLOR)),
            lineOpacity(palette.lineOpacity),
            lineWidth(2f),
        ),
    )
    add(
        SymbolLayer(ZONE_LABEL_LAYER, ZONE_LABEL_SOURCE).withProperties(
            iconImage(get(PROP_ICON)),
            // Keep overview labels readable without collisions; shop pins still draw above them.
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ).apply {
            maxZoom = ZONE_LABEL_MAX_ZOOM
        },
    )
    add(
        CircleLayer(PULSE_LAYER, PULSE_SOURCE).withProperties(
            circleColor("#EAB308"),
            circleRadius(PULSE_MIN_RADIUS),
            circleOpacity(0f),
            circleStrokeColor("#CA8A04"),
            circleStrokeWidth(1.5f),
            circleStrokeOpacity(0f),
            circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
        ),
    )
}

private fun syncZoneLayers(
    context: Context,
    style: Style,
    zones: List<MapCoffeeZone>,
    isDarkTheme: Boolean,
) {
    val shapes = zones.map { zone ->
        val color = zoneColorForMap(zone.color, isDarkTheme)
        Feature.fromGeometry(
            Polygon.fromLngLats(listOf(zoneOutline(zone).map { Point.fromLngLat(it.longitude, it.latitude) })),
        ).apply {
            addStringProperty(PROP_ZONE_ID, zone.id)
            addStringProperty(PROP_ZONE_COLOR, color)
        }
    }
    val labels = zones.map { zone ->
        val color = zoneColorForMap(zone.color, isDarkTheme)
        val icon = "zone-label-$isDarkTheme-$color-${zone.name}-${zone.shopCount}"
        if (style.getImage(icon) == null) {
            style.addImage(icon, MapMarkerIcons.zoneBitmap(context, zone.name, zone.shopCount, isDarkTheme, color))
        }
        Feature.fromGeometry(Point.fromLngLat(zone.longitude, zone.latitude)).apply {
            addStringProperty(PROP_ZONE_ID, zone.id)
            addStringProperty(PROP_ICON, icon)
        }
    }
    style.getSourceAs<GeoJsonSource>(ZONE_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(shapes))
    style.getSourceAs<GeoJsonSource>(ZONE_LABEL_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(labels))
}

private fun zoomToBounds(
    map: MapLibreMap,
    viewWidth: Int,
    viewHeight: Int,
    bounds: MapBounds,
) {
    val currentZoom = map.cameraPosition.zoom.toFloat()

    if (bounds.minLat == bounds.maxLat && bounds.minLon == bounds.maxLon) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(bounds.minLat, bounds.minLon),
                clusterFitZoom(currentZoom, currentZoom + 2f).toDouble(),
            ),
            CLUSTER_TAP_ANIMATION_MS.toInt(),
        )
        return
    }

    val latLngBounds = LatLngBounds.from(bounds.maxLat, bounds.maxLon, bounds.minLat, bounds.minLon)
    val insetX = (viewWidth.coerceAtLeast(1) * clusterPaddingFraction() / 2f).toInt()
    val insetY = (viewHeight.coerceAtLeast(1) * clusterPaddingFraction() / 2f).toInt()
    val fitted = map.getCameraForLatLngBounds(
        latLngBounds,
        intArrayOf(insetX, insetY, insetX, insetY),
    ) ?: return
    val target = CameraPosition.Builder(fitted)
        .zoom(clusterFitZoom(currentZoom, fitted.zoom.toFloat()).toDouble())
        .build()
    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(target),
        CLUSTER_TAP_ANIMATION_MS.toInt(),
    )
}

private fun zoneOutline(zone: MapCoffeeZone): List<LatLng> =
    if (zone.polygon.size >= 3) zone.polygon.map { (lat, lon) -> LatLng(lat, lon) }
    else zoneCirclePoints(zone)

private fun zoneCirclePoints(zone: MapCoffeeZone, pointCount: Int = 48): List<LatLng> {
    val latitudeDegrees = zone.radiusMeters / 111_320.0
    val longitudeScale = cos(Math.toRadians(zone.latitude)).coerceAtLeast(0.01)
    val longitudeDegrees = zone.radiusMeters / (111_320.0 * longitudeScale)
    return (0..pointCount).map { index ->
        val angle = 2.0 * Math.PI * index / pointCount
        LatLng(
            zone.latitude + latitudeDegrees * sin(angle),
            zone.longitude + longitudeDegrees * cos(angle),
        )
    }
}

private fun LatLngBounds.toMapBounds(): MapBounds = MapBounds(
    minLat = latitudeSouth,
    minLon = longitudeWest,
    maxLat = latitudeNorth,
    maxLon = longitudeEast,
)

private fun coffeeMapStyleUri(isDarkTheme: Boolean): String =
    if (isDarkTheme) OPEN_FREE_MAP_DARK_STYLE else OPEN_FREE_MAP_LIGHT_STYLE

private data class CoffeeMapPalette(
    val background: String,
    val residential: String,
    val park: String,
    val building: String,
    val water: String,
    val waterLine: String,
    val roadCasing: String,
    val road: String,
    val minorRoad: String,
    val boundary: String,
    val text: String,
    val waterText: String,
    val textHalo: String,
)

private fun applyCoffeePeekMapStyle(style: Style, isDarkTheme: Boolean) {
    val palette = if (isDarkTheme) {
        CoffeeMapPalette(
            background = "#1A1412",
            residential = "#211B18",
            park = "#25231D",
            building = "#302722",
            water = "#26343A",
            waterLine = "#42545B",
            roadCasing = "#332A26",
            road = "#4B403A",
            minorRoad = "#3D342F",
            boundary = "#66564D",
            text = "#C8BEB7",
            waterText = "#9AAFB5",
            textHalo = "#1A1412",
        )
    } else {
        CoffeeMapPalette(
            background = "#F8F6F3",
            residential = "#EFEAE5",
            park = "#E4E8DF",
            building = "#E3DDD7",
            water = "#D3DEE1",
            waterLine = "#B2C5CA",
            roadCasing = "#D6CEC7",
            road = "#FFFFFF",
            minorRoad = "#E8E2DC",
            boundary = "#AA9D94",
            text = "#625A55",
            waterText = "#687E84",
            textHalo = "#FAF8F5",
        )
    }

    style.layers.forEach { layer ->
        runCatching {
            val id = layer.id.lowercase()
            if (isMapNoiseLayer(id)) {
                layer.setProperties(visibility(Property.NONE))
                return@runCatching
            }

            if (id.contains("highway_path")) {
                layer.minZoom = maxOf(layer.minZoom, 15f)
            }

            when (layer) {
                is BackgroundLayer -> layer.setProperties(backgroundColor(palette.background))
                is FillLayer -> when {
                    id == "water" || id.startsWith("water_") -> layer.setProperties(
                        fillColor(palette.water),
                        fillOutlineColor(palette.waterLine),
                    )
                    id == "park" || id.contains("landcover_wood") || id.contains("landuse_park") -> {
                        layer.setProperties(fillColor(palette.park))
                    }
                    id.contains("building") -> layer.setProperties(
                        fillColor(palette.building),
                        fillOutlineColor(palette.roadCasing),
                    )
                    id.contains("residential") -> layer.setProperties(fillColor(palette.residential))
                }
                is LineLayer -> when {
                    id.contains("waterway") -> layer.setProperties(lineColor(palette.waterLine))
                    id.contains("boundary") -> layer.setProperties(lineColor(palette.boundary))
                    id.contains("highway") || id.contains("road") || id.contains("bridge") || id.contains("tunnel") -> {
                        val color = when {
                            id.contains("casing") -> palette.roadCasing
                            id.contains("inner") || id.contains("motorway") || id.contains("major") -> palette.road
                            else -> palette.minorRoad
                        }
                        layer.setProperties(lineColor(color))
                    }
                }
                is SymbolLayer -> layer.setProperties(
                    textColor(if (id.contains("water")) palette.waterText else palette.text),
                    textHaloColor(palette.textHalo),
                    textHaloWidth(1.2f),
                )
            }
        }
    }
}

private fun isMapNoiseLayer(id: String): Boolean {
    val segments = id.split('-', '_', '.')
    if (segments.any { it == "poi" || it == "housenumber" || it == "oneway" }) return true
    return mapNoiseLayerPhrases.any(id::contains)
}

private val mapNoiseLayerPhrases = listOf(
    "house-number",
    "house_number",
    "transit_stop",
    "transit-stop",
    "bus_stop",
    "bus-stop",
    "ferry_terminal",
    "ferry-terminal",
    "aerodrome_label",
    "aerodrome-label",
    "airport_label",
    "airport-label",
)
