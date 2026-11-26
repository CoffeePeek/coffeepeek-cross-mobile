package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.notifications.NativePushBridge
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.data.createNotificationRepository
import com.coffeepeek.feature.notifications.impl.createNotificationFeature
import org.koin.core.module.Module

internal fun Module.registerNotifications() {
    single<NotificationFeature> {
        createNotificationFeature(
            createNotificationRepository(get<ApiClients>().client),
            SessionNotificationAccount(get()),
            NativePushBridge,
            PreferenceNotificationCity(get()),
        )
    }
}
