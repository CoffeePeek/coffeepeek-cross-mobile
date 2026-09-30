package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsData
import com.coffeepeek.feature.shop.domain.model.MenuGallery
import com.coffeepeek.feature.shop.domain.model.MenuPhoto

internal fun ShopDetailsData.toMenuGallery(): MenuGallery = MenuGallery(
    shopTitle = shop.name.orEmpty(),
    photos = (shop.menu ?: menu)?.photos.orEmpty()
        .mapNotNull { photo ->
            val fullUrl = photo.fullScreenUrl() ?: return@mapNotNull null
            MenuPhoto(
                id = photo.id,
                fullUrl = fullUrl,
                previewUrl = photo.detailUrl(fullUrl),
                sortIndex = photo.sortIndex,
            )
        }
        .sortedBy(MenuPhoto::sortIndex),
)
