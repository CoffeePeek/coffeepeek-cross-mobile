package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.coffeepeek.admin.location.GeoPoint as PlatformPoint

/** CoreLocation and geocoding use the main run loop; permission requests stay with the form. */
internal class AppContributionLocation(
    private val hasPermission: () -> Boolean = PlatformLocation::hasPermission,
    private val lastLocation: suspend () -> PlatformPoint? = PlatformLocation::getLastKnownLocation,
    private val reverseGeocode: suspend (Double, Double) -> String? = PlatformLocation::reverseGeocode,
) : ContributionLocation {
    override suspend fun lastKnown(): GeoPoint? = withContext(Dispatchers.Main.immediate) {
        if (!hasPermission()) return@withContext null
        lastLocation()?.takeIf { hasPermission() }?.let { GeoPoint(it.latitude, it.longitude) }
    }

    // A manually picked map point can be resolved without granting device-location permission.
    override suspend fun addressOf(point: GeoPoint): String? = withContext(Dispatchers.Main.immediate) {
        reverseGeocode(point.latitude, point.longitude)
    }
}
