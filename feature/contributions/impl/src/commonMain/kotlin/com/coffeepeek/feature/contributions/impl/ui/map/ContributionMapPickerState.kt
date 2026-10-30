package com.coffeepeek.feature.contributions.impl.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.abs

internal data class ContributionMapUiState(
    val point: GeoPoint? = null,
    val cameraTarget: GeoPoint? = point,
    val locationRequest: Int = 0,
    val address: String? = null,
    val resolvedPoint: GeoPoint? = null,
    val isResolving: Boolean = false,
    val failed: Boolean = false,
) {
    val hasCurrentAddress: Boolean get() = point != null && point == resolvedPoint && !address.isNullOrBlank()
}

/** Ephemeral modal presentation state; disposed with this form/account, never a global location cache. */
internal class ContributionMapPickerState(initial: GeoPoint?) {
    var state by mutableStateOf(ContributionMapUiState(point = initial))
        private set

    fun move(point: GeoPoint) {
        if (!point.latitude.isFinite() || !point.longitude.isFinite() || point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return
        val current = state.point
        if (current == null || abs(current.latitude - point.latitude) > 0.000001 || abs(current.longitude - point.longitude) > 0.000001) {
            state = state.copy(point = point)
        }
    }

    fun cameraApplied() { state = state.copy(cameraTarget = null) }
    fun requestLocation() { state = state.copy(locationRequest = state.locationRequest + 1) }

    suspend fun resolve(location: ContributionLocation) {
        val point = state.point ?: return
        state = state.copy(isResolving = true, failed = false)
        try {
            delay(350)
            val address = location.addressOf(point)
            currentCoroutineContext().ensureActive()
            resolved(point, address)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            resolved(point, null)
        } finally {
            if (state.point == point) state = state.copy(isResolving = false)
        }
    }

    private fun resolved(point: GeoPoint, address: String?) {
        if (state.point == point) state = state.copy(address = address, resolvedPoint = point, failed = address.isNullOrBlank())
    }

    fun selection(): Pair<GeoPoint, String>? {
        if (state.isResolving || !state.hasCurrentAddress) return null
        return (state.point ?: return null) to state.address.orEmpty().trim()
    }
}
