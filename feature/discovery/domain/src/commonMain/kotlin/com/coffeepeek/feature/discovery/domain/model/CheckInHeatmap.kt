package com.coffeepeek.feature.discovery.domain.model

/** Aggregated public visits at a shop, never the visitor's device location. */
data class CheckInHeatPoint(val shopId: String, val coordinates: Coordinates, val checkInCount: Int)

data class CheckInHeatmap(val points: List<CheckInHeatPoint> = emptyList(), val isTruncated: Boolean = false)
