package com.coffeepeek.admin.di.notifications

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationsEntry
import com.coffeepeek.feature.notifications.impl.createNotificationsEntry

/** Android and iOS compose the migrated bell and sheet; signing in is the application's navigation. */
internal class FeatureNotificationsRenderer(private val entry: NotificationsEntry) : NotificationsRenderer {
    @Composable
    override fun Bell() = entry.Bell()
}

internal fun featureNotificationsRenderer(feature: NotificationFeature): NotificationsRenderer =
    FeatureNotificationsRenderer(createNotificationsEntry(feature, onSignIn = { Navigator.navigate(Navigator.Screen.Auth) }))
