package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.repository.ShopFavorites
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository

internal fun shopFavorites(repository: FavoritesRepository): ShopFavorites = object : ShopFavorites {
    override fun observeIds() = repository.observeIds()
    override suspend fun save(shop: ShopDetails) = repository.save(shop.toFavoriteSnapshot())
    override suspend fun remove(shopId: String) = repository.remove(shopId)
}

/** Application-owned conversion between independent feature contracts. */
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
