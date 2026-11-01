package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.runtime.Composable

/** Platform selection only. iOS keeps the legacy bell and sheet; Android composes the migrated entry. */
interface NotificationsRenderer {
    @Composable
    fun Bell()

    @Composable
    fun Settings(onBack: () -> Unit)

    @Composable
    fun HandlePendingOpen(isLoggedIn: Boolean) = Unit
}

internal object LegacyNotificationsRenderer : NotificationsRenderer {
    @Composable
    override fun Bell() = NotificationsButton()

    @Composable
    override fun Settings(onBack: () -> Unit) = NotificationSettingsScreen(onBack = onBack)
}
