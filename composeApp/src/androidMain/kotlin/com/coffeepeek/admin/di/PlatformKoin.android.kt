package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.admin.di.shop.shopFavoriteIntent
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer
import com.coffeepeek.admin.di.coffee.androidCoffeeScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.roaster.androidRoasterScreenRenderer
import com.coffeepeek.admin.di.roaster.roasterModule
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
            roasterModule(),
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
        roasterRendererFactory = { androidRoasterScreenRenderer(get(), get(), get()) },
    )
}
