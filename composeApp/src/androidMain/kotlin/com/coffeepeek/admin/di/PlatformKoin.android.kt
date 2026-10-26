package com.coffeepeek.admin.di

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shop.component.ShopMenuGalleryLink
import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.admin.di.shop.shopFavoriteIntent
import com.coffeepeek.admin.di.shop.AndroidShopCheckInDateFormatter
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.FeatureShopCheckInFormRenderer
import com.coffeepeek.admin.di.coffee.featureCoffeeScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.appupdate.androidAppUpdateRenderer
import com.coffeepeek.admin.di.auth.androidAuthScreenRenderer
import com.coffeepeek.admin.di.community.featureCommunityScreenRenderer
import com.coffeepeek.admin.di.community.featureCheckInDetailScreenRenderer
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.contributions.contributionsModule
import com.coffeepeek.admin.di.contributions.featureContributionsScreenRenderer
import com.coffeepeek.admin.di.discovery.featureDiscoveryScreenRenderer
import com.coffeepeek.admin.di.profile.featureAccountScreenRenderer
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.profile.featureOwnProfileScreenRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.di.profile.featurePublicProfileScreenRenderer
import com.coffeepeek.admin.di.shop.FeatureShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.FeatureShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.contentreport.contentReportModule
import com.coffeepeek.admin.di.contentreport.FeatureContentReportRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import android.widget.Toast
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
            shopCheckInModule(AndroidShopCheckInDateFormatter()),
            shopBrowsingModule(),
            homeCheckInModule(),
            checkInHistoryModule(),
            contentReportModule(),
            coffeeModule(),
            contributionsModule(),
        ) + extraModules,
        shopReportRendererFactory = { client ->
            FeatureShopReportScreenRenderer(
                createShopReportEntry(createShopIssueReportRepository(client.client)),
            )
        },
        shopGalleryRendererFactory = { client ->
            FeatureShopMenuGalleryScreenRenderer(
                createShopMenuGalleryEntry(createShopMenuGalleryRepository(client.client)),
                legacyAction = { id -> ShopMenuGalleryLink(onOpen = { Navigator.navigate(Navigator.Screen.ShopMenuGallery(id)) }) },
            )
        },
        shopCheckInRendererFactory = { FeatureShopCheckInFormRenderer(get()) },
        shopDetailRendererFactory = {
            FeatureShopDetailScreenRenderer(sessions = get(), accounts = get(), details = get(), votes = get(),
                favorites = get(), favoriteIntent = shopFavoriteIntent(get(), get(), get()), checkInForm = get(),
                visibility = get(), blockActions = get())
        },
        shopCheckInListRendererFactory = {
            FeatureShopCheckInListScreenRenderer(sessions = get(), accounts = get(), details = get(), votes = get(),
                visibility = get(), blockActions = get())
        },
        checkInRenderers = featureCheckInRendererFactories(),
        contentReportRendererFactory = { get<FeatureContentReportRenderer>() },
        coffeeRendererFactory = { featureCoffeeScreenRenderer(get()) },
        publicProfileRendererFactory = { featurePublicProfileScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { featureOwnProfileScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get(), get()) },
        accountRendererFactory = { featureAccountScreenRenderer(get(), get(), get()) },
        settingsRendererFactory = {
            featureSettingsScreenRenderer(get(), get(), get(), get(), notify = { Toast.makeText(Locator.appContext, it, Toast.LENGTH_SHORT).show() })
        },
        appUpdateRendererFactory = { androidAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get(), get(), get()) },
        authRendererFactory = { androidAuthScreenRenderer(get(), get()) },
        checkInDetailRendererFactory = { featureCheckInDetailScreenRenderer(get(), get(), get(), get()) },
        communityRendererFactory = { featureCommunityScreenRenderer(get(), get(), get(), get(), get(), get()) },
        contributionsRendererFactory = { featureContributionsScreenRenderer(get(), get(), get()) },
        discoveryRendererFactory = { featureDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get(), get()) },
    )
}
