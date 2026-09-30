package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.MenuGalleryData
import com.coffeepeek.feature.shop.domain.model.MenuGallery
import com.coffeepeek.feature.shop.domain.model.MenuPhoto

internal fun MenuGalleryData.toDomain(): MenuGallery = MenuGallery(
    shopTitle = shop.name.orEmpty(),
    photos = (shop.menu ?: menu)?.photos.orEmpty()
        .mapNotNull { photo ->
            val fullUrl = photo.urls?.fullscreen?.takeIf(String::isNotBlank)
                ?: photo.fullUrl?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            MenuPhoto(
                id = photo.id,
                fullUrl = fullUrl,
                previewUrl = photo.urls?.detail?.takeIf(String::isNotBlank) ?: fullUrl,
                sortIndex = photo.sortIndex,
            )
        }
        .sortedBy(MenuPhoto::sortIndex),
)
