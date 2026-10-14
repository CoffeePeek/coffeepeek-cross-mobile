package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.favoritesRoomModule
import com.coffeepeek.admin.di.favorites.legacyFavoritesConsumersModule
import com.coffeepeek.admin.di.shop.shopCheckInModule
import com.coffeepeek.admin.di.shop.AndroidShopCheckInFormRenderer
import com.coffeepeek.admin.di.shop.AndroidShopDetailScreenRenderer
import com.coffeepeek.admin.di.shop.AndroidShopCheckInListScreenRenderer
import com.coffeepeek.admin.di.shop.shopBrowsingModule
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistoryScreenRenderer
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.createShopReportEntry
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createShopMenuGalleryEntry

actual fun initPlatformKoin() {
    initKoin(
        registerLegacyFavorites = false,
        platformModules = listOf(
            favoritesRoomModule(Locator.database.settingRepository),
            legacyFavoritesConsumersModule(),
            shopCheckInModule(),
            shopBrowsingModule(),
            checkInHistoryModule(),
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
                favorites = get(), checkInForm = get())
        },
        shopCheckInListRendererFactory = {
            AndroidShopCheckInListScreenRenderer(sessions = get(), users = get(), details = get(), votes = get())
        },
        checkInHistoryRendererFactory = { get<AndroidCheckInHistoryScreenRenderer>() },
    )
}
