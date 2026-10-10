package com.coffeepeek.feature.favorites.domain.model

/** A saved snapshot, not the full catalog/details aggregate or an HTTP representation. */
data class FavoriteShop(
    val id: String,
    val title: String,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val cityName: String? = null,
    val priceRange: String? = null,
    val photoUrl: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isOpen: Boolean = false,
    val tags: List<String> = emptyList(),
    val brewMethods: List<String> = emptyList(),
    val roasterPhotoUrls: List<String> = emptyList(),
)
