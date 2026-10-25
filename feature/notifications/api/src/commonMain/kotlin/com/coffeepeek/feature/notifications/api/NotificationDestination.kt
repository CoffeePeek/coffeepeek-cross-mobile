package com.coffeepeek.feature.notifications.api

/** Owned notification targets; application composition resolves cross-feature routes. */
sealed interface NotificationDestination {
    data class Shop(val slug: String) : NotificationDestination
    data class ShopChange(val requestId: String) : NotificationDestination
    data class ShopSubmission(val submissionId: String) : NotificationDestination
    data object RoasterSubmissions : NotificationDestination
    data object CheckIns : NotificationDestination
}
