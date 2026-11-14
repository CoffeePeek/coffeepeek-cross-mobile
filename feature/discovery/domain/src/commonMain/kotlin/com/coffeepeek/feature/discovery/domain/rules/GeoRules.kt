package com.coffeepeek.feature.discovery.domain.rules

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** The distance over the surface in meters, or null when a point is not a place on Earth. */
fun distanceMeters(from: Coordinates?, to: Coordinates?): Double? {
    if (from == null || to == null || !from.isValid || !to.isValid) return null
    val latitudeDelta = (to.latitude - from.latitude).toRadians()
    val longitudeDelta = (to.longitude - from.longitude).toRadians()
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(from.latitude.toRadians()) * cos(to.latitude.toRadians()) * sin(longitudeDelta / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

private fun Double.toRadians(): Double = this * PI / 180.0
