package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.serialization.FlexibleIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ShopSearchResponseDto(
    val items: List<ShopSearchItemDto> = emptyList(),
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 1,
)

@Serializable
data class ShopSearchItemDto(
    val address: PublicAddressDto,
    val name: String,
    val addressLine: String? = null,
    val coverPhoto: ShortPhotoDto? = null,
    val rating: Double? = null,
    @Serializable(with = FlexibleIntSerializer::class) val checkInCount: Int = 0,
    val isOpen: Boolean = false,
    val isVisited: Boolean? = null,
    val isFavorite: Boolean? = null,
    val distanceMeters: Double? = null,
    val tags: List<CatalogItemDto> = emptyList(),
)
