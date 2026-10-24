package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.runtime.Composable

/** Platform selection only. iOS keeps the legacy bell and sheet; Android composes the migrated entry. */
interface NotificationsRenderer {
    @Composable
    fun Bell()
}

internal object LegacyNotificationsRenderer : NotificationsRenderer {
    @Composable
    override fun Bell() = NotificationsButton()
}
