package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopMenuDto
import com.coffeepeek.feature.shop.data.backend.ShopPhotoDto
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

internal fun ShopMenuDto.toDomain(): ShopMenu = ShopMenu(
    capturedAtUtc = capturedAtUtc,
    updatedAtUtc = updatedAtUtc,
    currency = currency.ifBlank { "BYN" },
    items = items.map { item ->
        ShopMenuItem(
            slug = item.slug,
            nameRu = item.nameRu,
            nameEn = item.nameEn,
            category = item.category,
            availability = item.availability,
            price = item.price.asDoubleOrNull(),
            currency = item.currency.ifBlank { "BYN" },
            volumeMl = item.volumeMl.asIntOrNull(),
        )
    },
    photos = photos.toMenuPhotos(),
)

internal fun List<ShopPhotoDto>.toMenuPhotos(): List<MenuPhoto> = mapNotNull { photo ->
    val fullUrl = photo.fullScreenUrl() ?: return@mapNotNull null
    MenuPhoto(
        id = photo.id,
        fullUrl = fullUrl,
        previewUrl = photo.detailUrl(fullUrl),
        sortIndex = photo.sortIndex,
    )
}.sortedBy(MenuPhoto::sortIndex)

private fun JsonElement?.asDoubleOrNull(): Double? = (this as? JsonPrimitive)?.let { primitive ->
    primitive.doubleOrNull ?: primitive.contentOrNull?.trim()?.replace(',', '.')?.toDoubleOrNull()
}

private fun JsonElement?.asIntOrNull(): Int? = (this as? JsonPrimitive)?.let { primitive ->
    primitive.contentOrNull?.trim()?.toIntOrNull() ?: primitive.doubleOrNull?.toInt()
}
