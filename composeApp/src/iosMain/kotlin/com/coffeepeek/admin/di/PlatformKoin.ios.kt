package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.featureAppUpdateRenderer
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.utils.ErrorHandler

actual fun initPlatformKoin() {
    initKoin(
        // Owners move to their migrated modules one at a time; the others keep the legacy renderers.
        settingsRendererFactory = {
            // iOS has no toast: the short message goes through the app's message channel.
            featureSettingsScreenRenderer(get(), get(), get(), get(), notify = ErrorHandler::showError)
        },
        appUpdateRendererFactory = { featureAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get()) },
    )
}
