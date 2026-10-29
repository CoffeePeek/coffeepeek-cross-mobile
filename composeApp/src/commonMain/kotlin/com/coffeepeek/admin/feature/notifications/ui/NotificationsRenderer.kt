package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface NotificationsRenderer {
    @Composable
    fun Bell()

    @Composable
    fun HandlePendingOpen(isLoggedIn: Boolean) = Unit
}
