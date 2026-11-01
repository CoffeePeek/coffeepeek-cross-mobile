package com.coffeepeek.feature.notifications.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationsEntry
import com.coffeepeek.feature.notifications.impl.ui.NotificationsButton
import com.coffeepeek.feature.notifications.impl.ui.NotificationSettingsScreen

/** The bell and sheet over [feature]; [onSignIn] opens the sign-in flow when a signed-out user taps the button in the sheet. */
fun createNotificationsEntry(
    feature: NotificationFeature,
    onSignIn: () -> Unit,
    onOpen: suspend (id: String, accountId: String, generation: Long) -> Boolean,
): NotificationsEntry = object : NotificationsEntry {
    @Composable
    override fun Bell() = NotificationsButton(feature, onSignIn, onOpen)

    @Composable
    override fun Settings(onBack: () -> Unit) = NotificationSettingsScreen(feature, onSignIn, onBack)
}
