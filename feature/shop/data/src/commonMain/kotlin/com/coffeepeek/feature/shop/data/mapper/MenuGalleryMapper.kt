package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import com.coffeepeek.feature.shop.domain.model.MenuGallery

internal fun ShopDetailsDto.toMenuGallery(): MenuGallery = MenuGallery(
    shopTitle = name.orEmpty(),
    photos = menu?.photos.orEmpty().toMenuPhotos(),
)
