package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.MenuGallery

interface ShopMenuGalleryRepository {
    suspend fun getMenuGallery(shopId: String): Result<MenuGallery>
}
