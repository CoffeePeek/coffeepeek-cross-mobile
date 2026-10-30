package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.admin.di.shop.shopFavoriteIntent
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer
import com.coffeepeek.admin.di.coffee.coffeeCatalogueScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.coffee.CoffeeShelfTab
import com.coffeepeek.admin.di.shelf.ShelfFeatureScreenRenderer
import com.coffeepeek.admin.di.shelf.shelfBrewForm
import com.coffeepeek.admin.di.shelf.shelfModule
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.di.appupdate.androidAppUpdateRenderer
import com.coffeepeek.admin.di.auth.androidAuthScreenRenderer
import com.coffeepeek.admin.di.community.checkInDetailFeatureScreenRenderer
import com.coffeepeek.admin.di.community.communityFeatureScreenRenderer
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.contributions.androidContributionsScreenRenderer
import com.coffeepeek.admin.di.discovery.androidDiscoveryScreenRenderer
import com.coffeepeek.admin.di.contributions.contributionsModule
import com.coffeepeek.admin.di.profile.accountFeatureScreenRenderer
import com.coffeepeek.admin.di.notifications.androidNotificationsRenderer
import com.coffeepeek.admin.di.profile.ownProfileFeatureScreenRenderer
import com.coffeepeek.admin.di.settings.androidSettingsScreenRenderer
import com.coffeepeek.admin.di.profile.publicProfileFeatureScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.checkinhistory.CheckInHistoryFeatureScreenRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
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
            homeCheckInModule(BuildConfig.SHELF_ENABLED),
            checkInHistoryModule(BuildConfig.SHELF_ENABLED),
            coffeeModule(),
            contributionsModule(),
            shelfModule(),
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
        checkInHistoryRendererFactory = { get<CheckInHistoryFeatureScreenRenderer>() },
        checkInCreateFlowRendererFactory = { get<FeatureCheckInCreateFlowRenderer>() },
        // Shelf: Coffee tab switcher, «+» scan flow, and shelf screens (live API on debug/release; Sandbox uses fake backend).
        coffeeRendererFactory = {
            coffeeCatalogueScreenRenderer(get(), CoffeeShelfTab(get(), get(), Locator.database.settingRepository).takeIf { BuildConfig.SHELF_ENABLED })
        },
        shelfRendererFactory = { ShelfFeatureScreenRenderer(get(), Locator.database.settingRepository, shelfBrewForm(get())) },
        publicProfileRendererFactory = { publicProfileFeatureScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { ownProfileFeatureScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get()) },
        accountRendererFactory = { accountFeatureScreenRenderer(get(), get()) },
        settingsRendererFactory = { androidSettingsScreenRenderer(get(), get(), get(), get()) },
        appUpdateRendererFactory = { androidAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { androidNotificationsRenderer(get(), get(), get()) },
        authRendererFactory = { androidAuthScreenRenderer(get(), get()) },
        communityRendererFactory = { communityFeatureScreenRenderer(get(), get(), get(), get(), get(), get()) },
        checkInDetailRendererFactory = { checkInDetailFeatureScreenRenderer(get(), get(), get(), get()) },
        contributionsRendererFactory = {
            androidContributionsScreenRenderer(get(), get(), get())
        },
        discoveryRendererFactory = { androidDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get()) },
    )
}
