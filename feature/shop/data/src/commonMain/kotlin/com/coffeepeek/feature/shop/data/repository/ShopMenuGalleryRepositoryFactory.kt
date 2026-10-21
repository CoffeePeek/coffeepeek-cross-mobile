package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.mapper.toMenuGallery
import com.coffeepeek.feature.shop.domain.model.MenuGallery
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import io.ktor.client.HttpClient

/** Reuses the application's configured authenticated client; the app owns its lifetime. */
fun createShopMenuGalleryRepository(client: HttpClient): ShopMenuGalleryRepository =
    DefaultShopMenuGalleryRepository(ShopDetailsBackend(client))

private class DefaultShopMenuGalleryRepository(
    private val backend: ShopDetailsBackend,
) : ShopMenuGalleryRepository {
    override suspend fun getMenuGallery(shopId: String): Result<MenuGallery> =
        backend.load(shopId).map { it.toMenuGallery() }
}
