package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopPhoto
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun ShopDetailsDto.toOverview(): ShopOverview = ShopOverview(
    id = publicAddress.slug,
    title = name.orEmpty(),
    description = description?.trim()?.takeIf(String::isNotEmpty),
    address = location?.address?.trim()?.takeIf(String::isNotEmpty),
    latitude = location?.latitude,
    longitude = location?.longitude,
    rating = rating.takeIf { it > 0.0 },
    reviewCount = checkInCount.toFlexibleInt(),
    isOpen = isOpen,
    photos = photos.mapNotNull { photo ->
        val fullUrl = photo.fullScreenUrl() ?: return@mapNotNull null
        ShopPhoto(
            id = photo.id,
            previewUrl = photo.detailUrl(fullUrl),
            fullUrl = fullUrl,
        )
    },
    priceRange = priceRange.toPriceRangeLabel(),
    canonicalPath = publicAddress.canonicalPath,
)

private fun kotlinx.serialization.json.JsonElement?.toPriceRangeLabel(): String? =
    when ((this as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()) {
        "1", "cheap", "$" -> "$"
        "2", "moderate", "$$" -> "$$"
        "3", "expensive", "$$$" -> "$$$"
        "4", "luxury", "$$$$" -> "$$$$"
        else -> null
    }
