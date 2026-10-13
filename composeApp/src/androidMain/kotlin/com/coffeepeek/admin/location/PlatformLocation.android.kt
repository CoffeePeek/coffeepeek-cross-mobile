package com.coffeepeek.admin.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.coffeepeek.admin.locator.Locator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

actual object PlatformLocation {

    private val postalCodeRegex = Regex("""^\d{4,6}$""")
    private val postalCodeInTextRegex = Regex("""\b\d{4,6}\b""")

    actual fun hasPermission(): Boolean = hasLocationPermission(Locator.appContext)

    actual suspend fun getLastKnownLocation(): GeoPoint? = withContext(Dispatchers.IO) {
        val context = Locator.appContext
        if (!hasLocationPermission(context)) return@withContext null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        val cached = providers
            .mapNotNull { provider ->
                try { locationManager.getLastKnownLocation(provider) }
                catch (_: SecurityException) { null }
                catch (_: IllegalArgumentException) { null }
            }
            .maxByOrNull { it.time }
        if (cached != null && System.currentTimeMillis() - cached.time in 0..120_000) {
            return@withContext GeoPoint(cached.latitude, cached.longitude)
        }
        for (provider in providers) {
            if (!runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)) continue
            val current = withTimeoutOrNull(6_000) {
                suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val cancellation = CancellationSignal()
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    try {
                        LocationManagerCompat.getCurrentLocation(
                            locationManager, provider, cancellation, ContextCompat.getMainExecutor(context),
                        ) { location -> if (continuation.isActive) continuation.resume(location) }
                    } catch (_: SecurityException) {
                        if (continuation.isActive) continuation.resume(null)
                    } catch (_: IllegalArgumentException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
            if (current != null) return@withContext GeoPoint(current.latitude, current.longitude)
        }
        null
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val context = Locator.appContext
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            cont.resume(addresses.firstOrNull()?.toDisplayAddress())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                        ?.firstOrNull()
                        ?.toDisplayAddress()
                }
            }.getOrNull()
        }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun android.location.Address.toDisplayAddress(): String? {
        val street = thoroughfare?.trim()?.takeIf { it.isNotEmpty() }
        val house = subThoroughfare?.trim()?.takeIf { it.isNotEmpty() }
        val premisesName = premises?.trim()?.takeIf { it.isNotEmpty() }
        val feature = featureName?.trim()?.takeIf { name ->
            name.isNotEmpty() &&
                name != street &&
                name != house &&
                name != premisesName &&
                !name.equals(locality, ignoreCase = true) &&
                !name.equals(subAdminArea, ignoreCase = true) &&
                !name.equals(adminArea, ignoreCase = true) &&
                !name.equals(countryName, ignoreCase = true) &&
                !postalCodeRegex.matches(name)
        }

        val streetParts = buildList {
            if (street != null) add(street)
            if (house != null) add(house)
            else if (feature != null && street != null) add(feature)
        }
        val streetLine = streetParts.joinToString(", ").ifBlank { null }
        val extra = premisesName?.takeIf { it != streetLine }

        val compact = listOfNotNull(streetLine, extra).joinToString(", ").ifBlank { null }
        if (compact != null) return compact

        // Fallback: take the first address line and drop city / region / country / index.
        return getAddressLine(0)?.let(::stripCityRegionCountry)
    }

    private fun stripCityRegionCountry(raw: String): String? {
        var result = raw.trim()
        if (result.isEmpty()) return null

        // Drop postal codes like 220030
        result = result.replace(postalCodeInTextRegex, " ")

        val noiseTokens = listOf(
            "республика беларусь",
            "republic of belarus",
            "беларусь",
            "belarus",
            "минская область",
            "minsk region",
            "minsk oblast",
            "город минск",
            "г. минск",
            "г минск",
            "минск",
            "minsk",
        )
        noiseTokens.forEach { token ->
            result = result.replace(Regex("""(?i)\b${Regex.escape(token)}\b"""), " ")
        }

        return result
            .split(',')
            .map { it.trim().trimStart('.').trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
    }
}
