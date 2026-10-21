package com.coffeepeek.feature.favorites.data.local.model

import kotlinx.serialization.Serializable

/** Field names/defaults intentionally match legacy LocalFavoriteShopDto. Never exported. */
@Serializable
internal data class StoredFavorite(
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
    val roasterPhotoUrl: String? = null,
    val roasterPhotoUrls: List<String> = emptyList(),
)
