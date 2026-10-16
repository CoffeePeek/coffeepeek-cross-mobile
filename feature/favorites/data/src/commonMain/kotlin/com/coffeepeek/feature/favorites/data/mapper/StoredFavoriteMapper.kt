package com.coffeepeek.feature.favorites.data.mapper

import com.coffeepeek.feature.favorites.data.local.model.StoredFavorite
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop

internal fun StoredFavorite.toDomain() = FavoriteShop(
    id, title, rating, reviewCount, cityName, priceRange, photoUrl, address,
    latitude, longitude, isOpen, tags.toList(), brewMethods.toList(),
    roasterPhotoUrls.ifEmpty { listOfNotNull(roasterPhotoUrl) }.toList(),
)

internal fun FavoriteShop.toStored() = StoredFavorite(
    id, title, rating, reviewCount, cityName, priceRange, photoUrl, address,
    latitude, longitude, isOpen, tags.toList(), brewMethods.toList(),
    roasterPhotoUrls.firstOrNull(), roasterPhotoUrls.toList(),
)
