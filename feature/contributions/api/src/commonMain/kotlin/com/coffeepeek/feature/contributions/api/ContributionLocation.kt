package com.coffeepeek.feature.contributions.api

data class GeoPoint(val latitude: Double, val longitude: Double)

/** Where the user is, as the shop form needs it. It reads the position only when the permission is already granted and never asks for it. */
interface ContributionLocation {
    /** The last known position, or null (no permission, no position). */
    suspend fun lastKnown(): GeoPoint?

    /** The street address of a point, or null when none is found. */
    suspend fun addressOf(point: GeoPoint): String?
}
