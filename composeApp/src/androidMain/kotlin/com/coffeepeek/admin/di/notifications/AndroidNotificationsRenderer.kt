package com.coffeepeek.admin.di.notifications

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationsEntry
import com.coffeepeek.feature.notifications.impl.createNotificationsEntry

/** Android composes the migrated bell and sheet; signing in is the application's navigation. */
internal class AndroidNotificationsRenderer(private val entry: NotificationsEntry) : NotificationsRenderer {
    @Composable
    override fun Bell() = entry.Bell()
}

internal fun androidNotificationsRenderer(feature: NotificationFeature): NotificationsRenderer =
    AndroidNotificationsRenderer(createNotificationsEntry(feature, onSignIn = { Navigator.navigate(Navigator.Screen.Auth) }))
