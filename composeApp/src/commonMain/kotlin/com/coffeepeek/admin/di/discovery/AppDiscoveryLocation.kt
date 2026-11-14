package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** CoreLocation needs the main run loop. The app owns the scope; this reader never asks for permission. */
internal class AppDiscoveryLocation(
    private val scope: CoroutineScope,
    private val hasPermission: () -> Boolean = PlatformLocation::hasPermission,
    private val lastLocation: suspend () -> GeoPoint? = PlatformLocation::getLastKnownLocation,
) : DiscoveryLocation {
    private val position = MutableStateFlow<Coordinates?>(null)
    private var refreshJob: Job? = null
    override val current = position.asStateFlow()

    init { refresh() }

    override fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.Main.immediate) {
            val point = if (hasPermission()) {
                lastLocation()?.takeIf { hasPermission() }
                    ?.let { Coordinates(it.latitude, it.longitude).takeIf(Coordinates::isValid) }
            } else null
            ensureActive()
            position.value = point
        }
    }

    override fun updateFromMap(point: Coordinates) {
        if (!point.isValid || !hasPermission()) return
        refreshJob?.cancel()
        position.value = point
    }
}
