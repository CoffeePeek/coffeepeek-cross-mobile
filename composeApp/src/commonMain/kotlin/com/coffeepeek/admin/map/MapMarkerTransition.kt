package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapBounds

internal const val MARKER_TRANSITION_MS = 380L

internal data class MapMarkerTarget(
    val key: String,
    val latitude: Double,
    val longitude: Double,
    val bounds: MapBounds? = null,
)

/** Match the smallest containing group, as the web does when server cluster ids change. */
internal fun markerParent(target: MapMarkerTarget, candidates: List<MapMarkerTarget>): MapMarkerTarget? =
    candidates.filter { it.key != target.key && it.bounds?.contains(target) == true }
        .minByOrNull { it.bounds!!.run { maxLat - minLat + maxLon - minLon } }

private fun MapBounds.contains(target: MapMarkerTarget): Boolean =
    target.latitude in minLat..maxLat && target.longitude in minLon..maxLon
