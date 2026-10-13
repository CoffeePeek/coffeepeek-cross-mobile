package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coffeepeek.domain.model.ShopLocation
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
    LaunchedEffect(refreshKey) {
        location = null
        if (PlatformLocation.hasPermission()) {
            location = PlatformLocation.getLastKnownLocation()
        }
    }
    return location
}

internal fun distanceToShopMeters(userLocation: GeoPoint?, shopLocation: ShopLocation?): Double? {
    val from = userLocation ?: return null
    val toLatitude = shopLocation?.latitude ?: return null
    val toLongitude = shopLocation.longitude ?: return null
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
