package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.api.PublicShopSummary
import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Public-profile cards retain their existing cover variant and tag fallback order. */
internal fun ShopDetailsDto.toPublicSummary() = PublicShopSummary(
    photoUrl = photos.firstOrNull()?.let { it.urls?.card?.takeIf(String::isNotBlank) ?: it.fullUrl?.takeIf(String::isNotBlank) },
    description = description, address = location?.address, rating = rating.takeIf { it > 0 },
    reviewCount = checkInCount.toFlexibleInt(), isOpen = isOpen,
    tags = (tags.publicTagNames() + shopTags.publicTagNames()).map(String::trim).filter(String::isNotBlank).distinct()
        .ifEmpty { (brewMethods + coffeeBeans).mapNotNull { it.name?.takeIf(String::isNotBlank) } }.take(3),
)

private fun JsonElement?.publicTagNames(): List<String> = (this as? JsonArray)?.mapNotNull { item ->
    when (item) {
        is JsonPrimitive -> item.contentOrNull
        is JsonObject -> listOf("name", "title", "label", "value").firstNotNullOfOrNull {
            (item[it] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        }
        else -> null
    }
}.orEmpty()
