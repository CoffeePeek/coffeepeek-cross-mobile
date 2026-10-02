package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsData
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopPhoto
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun ShopDetailsData.toOverview(requestedId: String): ShopOverview = ShopOverview(
    id = shop.id.ifBlank { requestedId },
    title = shop.name.orEmpty(),
    description = shop.description?.trim()?.takeIf(String::isNotEmpty),
    address = shop.location?.address?.trim()?.takeIf(String::isNotEmpty),
    latitude = shop.location?.latitude,
    longitude = shop.location?.longitude,
    rating = shop.rating.takeIf { it > 0.0 },
    reviewCount = shop.reviewCount,
    isOpen = shop.isOpen,
    photos = shop.photos.mapNotNull { photo ->
        val fullUrl = photo.fullScreenUrl() ?: return@mapNotNull null
        ShopPhoto(
            id = photo.id,
            previewUrl = photo.detailUrl(fullUrl),
            fullUrl = fullUrl,
        )
    },
    priceRange = shop.priceRange.toPriceRangeLabel(),
)

private fun kotlinx.serialization.json.JsonElement?.toPriceRangeLabel(): String? =
    when ((this as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()) {
        "1", "cheap", "$" -> "$"
        "2", "moderate", "$$" -> "$$"
        "3", "expensive", "$$$" -> "$$$"
        "4", "luxury", "$$$$" -> "$$$$"
        else -> null
    }
