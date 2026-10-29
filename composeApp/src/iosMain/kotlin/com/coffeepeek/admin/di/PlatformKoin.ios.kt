package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.FeatureShopReportScreenRenderer
import com.coffeepeek.admin.di.FeatureShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.di.shop.AppShopCheckInDateFormatter
import com.coffeepeek.admin.di.shop.FeatureShopCheckInFormRenderer
import com.coffeepeek.admin.di.shop.FeatureShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.FeatureShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.shopFavoriteIntent
import org.koin.core.scope.Scope
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.contentreport.contentReportModule
import com.coffeepeek.admin.di.contentreport.FeatureContentReportRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.createShopReportEntry
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createShopMenuGalleryEntry
import com.coffeepeek.admin.di.appupdate.featureAppUpdateRenderer
import com.coffeepeek.admin.di.contributions.contributionsModule
import com.coffeepeek.admin.di.contributions.featureContributionsScreenRenderer
import com.coffeepeek.admin.di.auth.iosAuthScreenRenderer
import com.coffeepeek.admin.di.community.featureCommunityScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.coffee.featureCoffeeScreenRenderer
import com.coffeepeek.admin.di.profile.featureAccountScreenRenderer
import com.coffeepeek.admin.di.profile.featureOwnProfileScreenRenderer
import com.coffeepeek.admin.di.profile.featurePublicProfileScreenRenderer
import com.coffeepeek.admin.di.discovery.featureDiscoveryScreenRenderer
import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.notifications.featureNotificationsRenderer
import com.coffeepeek.admin.di.settings.featureSettingsScreenRenderer
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.di.coffee.CoffeeShelfTab
import com.coffeepeek.admin.di.shelf.ShelfFeatureScreenRenderer
import com.coffeepeek.admin.di.shelf.iosShelfEnabled
import com.coffeepeek.admin.di.shelf.iosShelfModule
import com.coffeepeek.admin.di.shelf.shelfBrewForm
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.ui.screen.shelf.UnsupportedShelfScreenRenderer

actual fun initPlatformKoin() {
    val shelf = iosShelfEnabled()
    initKoin(
        platformModules = listOf(contributionsModule(), serverFavoritesModule(), coffeeModule(), shopBrowsingModule(), shopCheckInModule(AppShopCheckInDateFormatter()), homeCheckInModule(shelfEnabled = shelf), checkInHistoryModule(shelfEnabled = shelf), contentReportModule(), iosShelfModule()),
        // Owners move to their migrated modules one at a time; the others keep the legacy renderers.
        checkInRenderers = featureCheckInRendererFactories(),
        contentReportRendererFactory = { get<FeatureContentReportRenderer>() },
        shopReportRendererFactory = ::iosShopReportRenderer,
        shopGalleryRendererFactory = ::iosShopGalleryRenderer,
        shopCheckInRendererFactory = { FeatureShopCheckInFormRenderer(get()) },
        shopDetailRendererFactory = { iosShopDetailRenderer() },
        shopCheckInListRendererFactory = { iosShopCheckInListRenderer() },
        settingsRendererFactory = { iosSettingsRenderer() },
        appUpdateRendererFactory = { featureAppUpdateRenderer(get(), get()) },
        notificationsRendererFactory = { featureNotificationsRenderer(get(), get(), get()) },
        authRendererFactory = { iosAuthScreenRenderer(get(), get()) },
        checkInDetailRendererFactory = { com.coffeepeek.admin.di.community.featureCheckInDetailScreenRenderer(get(), get(), get(), get()) },
        communityRendererFactory = { featureCommunityScreenRenderer(get(), get(), get(), get(), get(), get()) },
        coffeeRendererFactory = {
            featureCoffeeScreenRenderer(get(), CoffeeShelfTab(get(), get(), Locator.database.settingRepository).takeIf { shelf })
        },
        shelfRendererFactory = {
            if (shelf) ShelfFeatureScreenRenderer(get(), Locator.database.settingRepository, shelfBrewForm(get()))
            else UnsupportedShelfScreenRenderer
        },
        publicProfileRendererFactory = { featurePublicProfileScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { featureOwnProfileScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get(), get()) },
        contributionsRendererFactory = { featureContributionsScreenRenderer(get(), get(), get()) },
        accountRendererFactory = { featureAccountScreenRenderer(get(), get(), get()) },
        discoveryRendererFactory = { featureDiscoveryScreenRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get(), get(), get()) },
    )
}

private fun iosShopReportRenderer(client: CoffeePeekClient) =
    FeatureShopReportScreenRenderer(createShopReportEntry(createShopIssueReportRepository(client.client)))

private fun iosShopGalleryRenderer(client: CoffeePeekClient) =
    FeatureShopMenuGalleryScreenRenderer(createShopMenuGalleryEntry(createShopMenuGalleryRepository(client.client)))

private fun Scope.iosShopDetailRenderer() =
    FeatureShopDetailScreenRenderer(sessions = get(), accounts = get(), details = get(), votes = get(),
        favorites = get(), favoriteIntent = shopFavoriteIntent(get(), get(), get()), checkInForm = get(),
        visibility = get(), blockActions = get())

private fun Scope.iosShopCheckInListRenderer() =
    FeatureShopCheckInListScreenRenderer(sessions = get(), accounts = get(), details = get(), votes = get(),
        visibility = get(), blockActions = get())

private fun Scope.iosSettingsRenderer() =
    featureSettingsScreenRenderer(get(), get(), get(), get(), notify = ErrorHandler::showError)
