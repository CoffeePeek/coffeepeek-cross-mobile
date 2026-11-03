package com.coffeepeek.feature.notifications.api

import androidx.compose.runtime.Composable

/**
 * The notification bell with its unread badge. A tap opens the notification sheet.
 * The caller places it in the header it owns, for example the community feed.
 */
interface NotificationsEntry {
    @Composable
    fun Bell()

    /** The inline app-notification preference; the Settings tab supplies its label and description. */
    @Composable
    fun Preference(label: String, description: String)
}
