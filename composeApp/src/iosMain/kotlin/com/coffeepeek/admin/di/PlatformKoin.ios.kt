package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.featureAppUpdateRenderer
import com.coffeepeek.admin.di.auth.iosAuthScreenRenderer
import com.coffeepeek.admin.di.community.featureCommunityScreenRenderer
import com.coffeepeek.admin.di.discovery.featureDiscoveryScreenRenderer
import com.coffeepeek.admin.di.favorites.serverFavoriteShopsModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.utils.ErrorHandler

actual fun initPlatformKoin() {
    initKoin(
        platformModules = listOf(serverFavoriteShopsModule()),
        // Owners move to their migrated modules one at a time; the others keep the legacy renderers.
        settingsRendererFactory = {
            // iOS has no toast: the short message goes through the app's message channel.
            featureSettingsScreenRenderer(get(), get(), get(), get(), notify = ErrorHandler::showError)
        },
        appUpdateRendererFactory = { featureAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get()) },
        authRendererFactory = { iosAuthScreenRenderer(get(), get()) },
        communityRendererFactory = { featureCommunityScreenRenderer(get(), get(), get(), get(), get(), get()) },
        discoveryRendererFactory = { featureDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get(), get()) },
    )
}
