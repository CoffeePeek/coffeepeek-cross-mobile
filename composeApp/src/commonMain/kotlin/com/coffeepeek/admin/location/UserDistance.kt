package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Reads location only when permission has already been granted; this never opens a permission dialog. */
@Composable
internal fun rememberPermittedUserLocation(refreshKey: Int = 0): GeoPoint? {
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    var resumeKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeKey++ }
    LaunchedEffect(refreshKey, resumeKey) {
        location = null
        location = readPermittedUserLocation()
    }
    return location
}

/** Check on both sides of the suspend point so a revoked or cancelled read cannot restore stale distance. */
internal suspend fun readPermittedUserLocation(
    hasPermission: () -> Boolean = PlatformLocation::hasPermission,
    readLocation: suspend () -> GeoPoint? = PlatformLocation::getLastKnownLocation,
): GeoPoint? {
    if (!hasPermission()) return null
    val point = readLocation()
    currentCoroutineContext().ensureActive()
    return point.takeIf { hasPermission() }
}

internal fun distanceToShopMeters(userLocation: GeoPoint?, latitude: Double?, longitude: Double?): Double? {
    val from = userLocation ?: return null
    val toLatitude = latitude ?: return null
    val toLongitude = longitude ?: return null
    if (!from.latitude.isValidLatitude() || !from.longitude.isValidLongitude()) return null
    if (!toLatitude.isValidLatitude() || !toLongitude.isValidLongitude()) return null

    val latitudeDelta = (toLatitude - from.latitude).toRadians()
    val longitudeDelta = (toLongitude - from.longitude).toRadians()
    val fromLatitude = from.latitude.toRadians()
    val toLatitudeRadians = toLatitude.toRadians()
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(fromLatitude) * cos(toLatitudeRadians) *
        sin(longitudeDelta / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

internal fun formatDistance(distanceMeters: Double?): String? {
    val distance = distanceMeters?.takeIf { it.isFinite() && it >= 0 } ?: return null
    if (distance < 1_000) return "${distance.roundToInt()} м"

    val tenthsOfKilometer = (distance / 100).roundToInt()
    val kilometers = tenthsOfKilometer / 10
    val tenths = tenthsOfKilometer % 10
    return if (tenths == 0) "$kilometers км" else "$kilometers,$tenths км"
}

private fun Double.toRadians(): Double = this * PI / 180.0
private fun Double.isValidLatitude(): Boolean = isFinite() && this in -90.0..90.0
private fun Double.isValidLongitude(): Boolean = isFinite() && this in -180.0..180.0
