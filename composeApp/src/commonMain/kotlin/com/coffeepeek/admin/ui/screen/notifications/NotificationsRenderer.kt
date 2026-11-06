package com.coffeepeek.admin.ui.screen.notifications

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface NotificationsRenderer {
    @Composable
    fun Bell()

    @Composable
    fun Preference(label: String, description: String)

    @Composable
    fun HandlePendingOpen(isLoggedIn: Boolean) = Unit
}
