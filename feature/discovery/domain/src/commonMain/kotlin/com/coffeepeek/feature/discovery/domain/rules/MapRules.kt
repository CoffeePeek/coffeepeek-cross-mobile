package com.coffeepeek.feature.discovery.domain.rules

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

const val MAP_NEARBY_RADIUS_METERS = 5_000.0
const val MAP_NEARBY_LIMIT = 10
const val MIN_MAP_ZOOM = 3f
const val MAX_MAP_ZOOM = 20f

private const val KM_PER_DEGREE_LAT = 111.32
private const val PREFETCH_MIN_MARGIN_KM = 3.0
private const val PREFETCH_VIEWPORT_FRACTION = 0.5
private const val NEARBY_DEGREE_METERS = 110_000.0

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

/** The box around [origin] that the nearby carousel asks for: [MAP_NEARBY_RADIUS_METERS] on every side. */
fun nearbyBounds(origin: Coordinates): MapBounds {
    val latitudeMargin = MAP_NEARBY_RADIUS_METERS / NEARBY_DEGREE_METERS
    val longitudeMargin = latitudeMargin / cos((abs(origin.latitude) + latitudeMargin) * PI / 180).coerceAtLeast(0.01)
    return MapBounds(
        (origin.latitude - latitudeMargin).coerceAtLeast(-85.0),
        (origin.longitude - longitudeMargin).coerceAtLeast(-180.0),
        (origin.latitude + latitudeMargin).coerceAtMost(85.0),
        (origin.longitude + longitudeMargin).coerceAtMost(180.0),
    )
}

/** The [MAP_NEARBY_LIMIT] markers nearest to [origin] inside the radius, one per id, sorted by distance and then by id. */
fun nearestMarkers(markers: List<MapMarker>, origin: Coordinates): List<MapMarker> =
    markers.distinctBy { it.id }.mapNotNull { marker ->
        val distance = distanceMeters(origin, marker.coordinates) ?: return@mapNotNull null
        if (distance <= MAP_NEARBY_RADIUS_METERS) marker to distance else null
    }.sortedWith(compareBy<Pair<MapMarker, Double>> { it.second }.thenBy { it.first.id }).take(MAP_NEARBY_LIMIT).map { it.first }
