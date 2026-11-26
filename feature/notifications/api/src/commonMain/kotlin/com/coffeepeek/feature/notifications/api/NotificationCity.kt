package com.coffeepeek.feature.notifications.api

import kotlinx.coroutines.flow.Flow

/** The application's existing selected city. Observe passively; do not request location or permission. */
fun interface NotificationCity {
    /** Replays resolved choices only: no emission while the saved choice is still loading. */
    fun observeSelectedCity(): Flow<NotificationCitySelection>
}

/** A resolved city slug; explicit null means the current selection is empty. */
data class NotificationCitySelection(val citySlug: String?)
