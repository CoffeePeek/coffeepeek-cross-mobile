package com.coffeepeek.feature.notifications.api

import androidx.compose.runtime.Composable

/**
 * The notification bell with its unread badge. A tap opens the notification sheet.
 * The caller places it in the header it owns, for example the community feed.
 */
interface NotificationsEntry {
    @Composable
    fun Bell()

    /** The existing app-notification preference; the caller owns the Settings route and back action. */
    @Composable
    fun Settings(onBack: () -> Unit)
}
