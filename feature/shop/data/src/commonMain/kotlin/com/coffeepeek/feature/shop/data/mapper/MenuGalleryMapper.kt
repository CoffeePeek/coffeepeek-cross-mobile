package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsData
import com.coffeepeek.feature.shop.domain.model.MenuGallery

internal fun ShopDetailsData.toMenuGallery(): MenuGallery = MenuGallery(
    shopTitle = shop.name.orEmpty(),
    photos = (shop.menu ?: menu)?.photos.orEmpty().toMenuPhotos(),
)
