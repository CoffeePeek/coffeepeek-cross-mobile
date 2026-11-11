package com.coffeepeek.feature.discovery.impl.map

import com.coffeepeek.feature.discovery.api.MapHeatPoint

private const val CHECK_INS_AT_FULL_HEAT = 15.0
// At intensity 1, MapLibre's Gaussian kernel peaks at about 0.399 times the point weight.
private const val FULL_HEAT_WEIGHT = 2.6

/** A fixed scale keeps the same number of visits equally hot when panning. */
internal fun MapState.canvasHeatmap(isPreview: Boolean): List<MapHeatPoint> {
    if (!showHeatmap || isPreview) return emptyList()
    return heatmap.points.filter { it.checkInCount > 0 && it.coordinates.isValid }
        .map { MapHeatPoint(it.coordinates, it.checkInCount / CHECK_INS_AT_FULL_HEAT * FULL_HEAT_WEIGHT) }
}
