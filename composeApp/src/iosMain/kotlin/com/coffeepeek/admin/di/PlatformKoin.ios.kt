package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.featureAppUpdateRenderer
import com.coffeepeek.admin.di.auth.FeatureAuthScreenRenderer
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.feature.auth.impl.createLoginEntry
import com.coffeepeek.feature.auth.impl.createPasswordRecoveryEntry
import com.coffeepeek.feature.auth.impl.createRegisterEntry

actual fun initPlatformKoin() {
    initKoin(
        // Owners move to their migrated modules one at a time; the others keep the legacy renderers.
        settingsRendererFactory = {
            // iOS has no toast: the short message goes through the app's message channel.
            featureSettingsScreenRenderer(get(), get(), get(), get(), notify = ErrorHandler::showError)
        },
        appUpdateRendererFactory = { featureAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get()) },
        // Google and Apple sign-in have no launcher on iOS yet, so their buttons are not shown.
        authRendererFactory = {
            val repository = get<com.coffeepeek.feature.auth.domain.AuthRepository>()
            val analytics = get<com.coffeepeek.feature.auth.api.AuthAnalytics>()
            FeatureAuthScreenRenderer(
                login = createLoginEntry(repository, analytics, google = null),
                register = createRegisterEntry(repository, analytics, google = null),
                recovery = createPasswordRecoveryEntry(repository),
            )
        },
    )
}
