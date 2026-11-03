package com.coffeepeek.feature.discovery.domain.rules

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

const val MIN_MAP_ZOOM = 3f
const val MAX_MAP_ZOOM = 20f

private const val KM_PER_DEGREE_LAT = 111.32
private const val PREFETCH_MIN_MARGIN_KM = 3.0
private const val PREFETCH_VIEWPORT_FRACTION = 0.5
private const val CAROUSEL_MIN_RADIUS_KM = 1.5

fun clampZoom(zoom: Float): Float = zoom.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)

val MapBounds.center: Coordinates get() = Coordinates((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)

fun MapBounds.contains(other: MapBounds): Boolean =
    other.minLat >= minLat && other.maxLat <= maxLat && other.minLon >= minLon && other.maxLon <= maxLon

/**
 * The visible bounds padded on every side by half a viewport, but at least [minMarginKm], so that moving the map a couple of kilometers stays inside the loaded area.
 * It ignores the antimeridian (city maps) and clamps to ±85 / ±180.
 */
fun MapBounds.expandedForPrefetch(minMarginKm: Double = PREFETCH_MIN_MARGIN_KM): MapBounds {
    val midLat = (minLat + maxLat) / 2.0
    val kmPerDegreeLon = KM_PER_DEGREE_LAT * cos(midLat * PI / 180.0).coerceAtLeast(0.01)
    val latMargin = max((maxLat - minLat) * PREFETCH_VIEWPORT_FRACTION, minMarginKm / KM_PER_DEGREE_LAT)
    val lonMargin = max((maxLon - minLon) * PREFETCH_VIEWPORT_FRACTION, minMarginKm / kmPerDegreeLon)
    return MapBounds(
        minLat = (minLat - latMargin).coerceAtLeast(-85.0),
        minLon = (minLon - lonMargin).coerceAtLeast(-180.0),
        maxLat = (maxLat + latMargin).coerceAtMost(85.0),
        maxLon = (maxLon + lonMargin).coerceAtMost(180.0),
    )
}

/** Include shops near a tightly zoomed viewport without enlarging an already broad map view. */
fun MapBounds.expandedForCarousel(): MapBounds {
    if (minLon > maxLon) return this
    val middle = center
    val kmPerDegreeLon = KM_PER_DEGREE_LAT * cos(middle.latitude * PI / 180.0).coerceAtLeast(0.01)
    val viewportLatRadius = (maxLat - minLat) / 2.0
    val viewportLonRadius = (maxLon - minLon) / 2.0
    val minimumLatRadius = CAROUSEL_MIN_RADIUS_KM / KM_PER_DEGREE_LAT
    val minimumLonRadius = CAROUSEL_MIN_RADIUS_KM / kmPerDegreeLon
    if (viewportLatRadius >= minimumLatRadius && viewportLonRadius >= minimumLonRadius) return this
    val latRadius = max(viewportLatRadius, minimumLatRadius)
    val lonRadius = max(viewportLonRadius, minimumLonRadius)
    return MapBounds(
        minLat = (middle.latitude - latRadius).coerceAtLeast(-85.0),
        minLon = (middle.longitude - lonRadius).coerceAtLeast(-180.0),
        maxLat = (middle.latitude + latRadius).coerceAtMost(85.0),
        maxLon = (middle.longitude + lonRadius).coerceAtMost(180.0),
    )
}

/** Keep surviving visible cards in place; append newcomers nearest the viewport center. */
fun viewportMarkers(markers: List<MapMarker>, bounds: MapBounds, previous: List<MapMarker>): List<MapMarker> {
    val positions = previous.mapIndexed { index, marker -> marker.id to index }.toMap()
    val viewportCenter = bounds.center
    return markers.distinctBy { it.id }.filter { marker ->
        val point = marker.coordinates
        point.isValid && point.latitude in bounds.minLat..bounds.maxLat &&
            if (bounds.minLon <= bounds.maxLon) point.longitude in bounds.minLon..bounds.maxLon
            else point.longitude >= bounds.minLon || point.longitude <= bounds.maxLon
    }.sortedWith(compareBy<MapMarker> { positions[it.id] ?: Int.MAX_VALUE }
        .thenBy { distanceMeters(viewportCenter, it.coordinates) ?: Double.POSITIVE_INFINITY }
        .thenBy { it.id })
}
