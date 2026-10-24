package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.admin.di.shop.shopFavoriteIntent
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer
import com.coffeepeek.admin.di.coffee.androidCoffeeScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.appupdate.androidAppUpdateRenderer
import com.coffeepeek.admin.di.auth.androidAuthScreenRenderer
import com.coffeepeek.admin.di.community.androidCommunityScreenRenderer
import com.coffeepeek.api.CoffeePeekClient
import java.time.OffsetDateTime
import com.coffeepeek.admin.di.contributions.androidContributionsScreenRenderer
import com.coffeepeek.admin.di.discovery.androidDiscoveryScreenRenderer
import com.coffeepeek.feature.contributions.data.repository.createContributionsRepository
import com.coffeepeek.admin.di.profile.androidAccountScreenRenderer
import com.coffeepeek.admin.di.notifications.androidNotificationsRenderer
import com.coffeepeek.admin.di.profile.androidOwnProfileScreenRenderer
import com.coffeepeek.admin.di.settings.androidSettingsScreenRenderer
import com.coffeepeek.admin.di.profile.androidPublicProfileScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistoryScreenRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.homecheckin.AndroidCheckInCreateFlowRenderer
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.createShopReportEntry
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createShopMenuGalleryEntry

actual fun initPlatformKoin() = initAndroidKoin()

/** Variant-owned composition can replace external ports without forking the app graph. */
internal fun initAndroidKoin(extraModules: List<org.koin.core.module.Module> = emptyList(), analyticsEnabled: Boolean = true) {
    initKoin(
        analyticsEnabled = analyticsEnabled,
        registerLegacyFavorites = true,
        platformModules = listOf(
            serverFavoritesModule(),
            shopCheckInModule(),
            shopBrowsingModule(),
            homeCheckInModule(),
            checkInHistoryModule(),
            coffeeModule(),
        ) + extraModules,
        shopReportRendererFactory = { client ->
            AndroidShopReportScreenRenderer(
                createShopReportEntry(createShopIssueReportRepository(client.client)),
            )
        },
        shopGalleryRendererFactory = { client ->
            AndroidShopMenuGalleryScreenRenderer(
                createShopMenuGalleryEntry(createShopMenuGalleryRepository(client.client)),
            )
        },
        shopCheckInRendererFactory = { AndroidShopCheckInFormRenderer(get()) },
        shopDetailRendererFactory = {
            AndroidShopDetailScreenRenderer(sessions = get(), users = get(), details = get(), votes = get(),
                favorites = get(), favoriteIntent = shopFavoriteIntent(get(), get(), get()), checkInForm = get(),
                visibility = get(), blockActions = get())
        },
        shopCheckInListRendererFactory = {
            AndroidShopCheckInListScreenRenderer(sessions = get(), users = get(), details = get(), votes = get(),
                visibility = get(), blockActions = get())
        },
        checkInHistoryRendererFactory = { get<AndroidCheckInHistoryScreenRenderer>() },
        checkInCreateFlowRendererFactory = { get<AndroidCheckInCreateFlowRenderer>() },
        coffeeRendererFactory = { androidCoffeeScreenRenderer(get()) },
        publicProfileRendererFactory = { androidPublicProfileScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { androidOwnProfileScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get()) },
        accountRendererFactory = { androidAccountScreenRenderer(get(), get()) },
        settingsRendererFactory = { androidSettingsScreenRenderer(get(), get(), get(), get()) },
        appUpdateRendererFactory = { androidAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { androidNotificationsRenderer(get()) },
        authRendererFactory = { androidAuthScreenRenderer(get(), get()) },
        communityRendererFactory = { androidCommunityScreenRenderer(get(), get(), get(), get(), get(), get()) },
        contributionsRendererFactory = {
            val gateway = get<CoffeePeekClient>()
            androidContributionsScreenRenderer(
                createContributionsRepository(gateway.client, gateway.uploadClient) { OffsetDateTime.now().offset.totalSeconds / 60 }, get(), get(),
            )
        },
        discoveryRendererFactory = { androidDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get()) },
    )
}
