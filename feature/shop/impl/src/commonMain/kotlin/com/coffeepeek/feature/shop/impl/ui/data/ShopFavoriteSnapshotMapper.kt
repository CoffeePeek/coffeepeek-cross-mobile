package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.shop.domain.model.ShopDetails

/** Adapter to the intentionally supported pure favorites/domain contract. */
internal fun ShopDetails.toFavoriteSnapshot(): FavoriteShop = FavoriteShop(
    id = overview.id,
    title = overview.title,
    rating = overview.rating,
    reviewCount = overview.reviewCount,
    priceRange = overview.priceRange,
    photoUrl = overview.photos.firstOrNull()?.previewUrl,
    address = overview.address,
    latitude = overview.latitude,
    longitude = overview.longitude,
    isOpen = overview.isOpen,
    tags = features.filterNot { it.isBrewMethod }.map { it.name },
    brewMethods = features.filter { it.isBrewMethod }.map { it.name },
    roasterPhotoUrls = coffee.roasters.mapNotNull { it.photoUrl },
)
