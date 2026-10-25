package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.appupdate.featureAppUpdateRenderer
import com.coffeepeek.admin.di.contributions.featureContributionsScreenRenderer
import com.coffeepeek.admin.di.auth.iosAuthScreenRenderer
import com.coffeepeek.admin.di.community.featureCommunityScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.coffee.featureCoffeeScreenRenderer
import com.coffeepeek.admin.di.profile.featureAccountScreenRenderer
import com.coffeepeek.admin.di.profile.featureOwnProfileScreenRenderer
import com.coffeepeek.admin.di.profile.featurePublicProfileScreenRenderer
import com.coffeepeek.admin.di.discovery.featureDiscoveryScreenRenderer
import com.coffeepeek.admin.di.favorites.serverFavoriteShopsModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.utils.ErrorHandler

actual fun initPlatformKoin() {
    initKoin(
        platformModules = listOf(serverFavoriteShopsModule(), coffeeModule()),
        // Owners move to their migrated modules one at a time; the others keep the legacy renderers.
        settingsRendererFactory = {
            // iOS has no toast: the short message goes through the app's message channel.
            featureSettingsScreenRenderer(get(), get(), get(), get(), notify = ErrorHandler::showError)
        },
        appUpdateRendererFactory = { featureAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get()) },
        authRendererFactory = { iosAuthScreenRenderer(get(), get()) },
        communityRendererFactory = { featureCommunityScreenRenderer(get(), get(), get(), get(), get(), get()) },
        coffeeRendererFactory = { featureCoffeeScreenRenderer(get()) },
        publicProfileRendererFactory = { featurePublicProfileScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { featureOwnProfileScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get()) },
        contributionsRendererFactory = { featureContributionsScreenRenderer(get<CoffeePeekClient>(), get(), get()) },
        accountRendererFactory = { featureAccountScreenRenderer(get(), get()) },
        discoveryRendererFactory = { featureDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get(), get()) },
    )
}
