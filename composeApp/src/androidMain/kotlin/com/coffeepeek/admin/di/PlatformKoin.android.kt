package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.serverFavoritesModule
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer
import com.coffeepeek.admin.di.shop.AndroidShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.createShopReportEntry
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createShopMenuGalleryEntry

actual fun initPlatformKoin() {
    initKoin(
        registerLegacyFavorites = true,
        platformModules = listOf(
            serverFavoritesModule(),
            shopCheckInModule(),
            shopBrowsingModule(),
        ),
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
                favorites = get(), checkInForm = get(), visibility = get(), blockActions = get())
        },
        shopCheckInListRendererFactory = {
            AndroidShopCheckInListScreenRenderer(sessions = get(), users = get(), details = get(), votes = get(),
                visibility = get(), blockActions = get())
        },
    )
}
