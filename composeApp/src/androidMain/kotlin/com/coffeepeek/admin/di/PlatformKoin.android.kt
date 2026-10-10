package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.favorites.favoritesRoomModule
import com.coffeepeek.admin.di.favorites.legacyFavoritesConsumersModule
import com.coffeepeek.admin.di.shop.shopCheckInModule
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
    )
}
