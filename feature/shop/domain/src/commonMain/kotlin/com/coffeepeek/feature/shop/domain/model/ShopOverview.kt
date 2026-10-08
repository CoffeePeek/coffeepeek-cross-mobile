package com.coffeepeek.feature.shop.domain.model

/** Read-only details used by the shop header; interaction ownership is added later. */
data class ShopOverview(
    val id: String,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val rating: Double?,
    val reviewCount: Int,
    val isOpen: Boolean,
    val photos: List<ShopPhoto>,
    val priceRange: String? = null,
    val canonicalPath: String? = null,
)

data class ShopPhoto(
    val id: String,
    val previewUrl: String,
    val fullUrl: String,
)
