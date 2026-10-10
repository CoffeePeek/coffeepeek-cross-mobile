package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.domain.repository.FavoriteRepository as LegacyFavoriteRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException

/** Temporary bridge for existing consumers. Wire it to the SAME new repository instance as the new screen. */
internal fun createLegacyFavoritesRepositoryBridge(repository: FavoritesRepository): LegacyFavoriteRepository =
    LegacyFavoritesRepositoryBridge(repository)

private class LegacyFavoritesRepositoryBridge(
    private val repository: FavoritesRepository,
) : LegacyFavoriteRepository {
    override suspend fun getFavoriteIds(): Set<String> = repository.read().fold(
        onSuccess = { shops -> shops.mapTo(mutableSetOf()) { it.id } },
        onFailure = { error ->
            if (error is CancellationException) throw error
            // Legacy read-only queries historically treated malformed rows as empty.
            // Writes still fail through Result rather than overwriting those rows.
            emptySet()
        },
    )

    override suspend fun isFavorite(shopId: String): Boolean = shopId in getFavoriteIds()

    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> =
        repository.read().map { shops -> shops.map(FavoriteShop::toLegacyDetails) }

    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> =
        repository.save(shop.toFavoriteShop(address))

    override suspend fun removeFavorite(shopId: String): Result<Unit> = repository.remove(shopId)

    override suspend fun clearAll() {
        repository.clear().getOrThrow()
    }
}

private fun CoffeeShop.toFavoriteShop(addressOverride: String?) = FavoriteShop(
    id = id,
    title = title,
    rating = rating,
    reviewCount = reviewCount,
    cityName = cityName,
    priceRange = priceRange,
    photoUrl = photoUrl,
    address = addressOverride ?: address,
    latitude = location?.latitude,
    longitude = location?.longitude,
    isOpen = isOpen,
    tags = tags,
    brewMethods = brewMethods,
    roasterPhotoUrls = roasterPhotoUrls,
)

private fun FavoriteShop.toLegacyDetails(): CoffeeShopDetails {
    val location = if (address != null || latitude != null || longitude != null) {
        ShopLocation(address = address, latitude = latitude, longitude = longitude)
    } else null
    return CoffeeShopDetails(
        shop = CoffeeShop(
            id = id,
            title = title,
            rating = rating,
            reviewCount = reviewCount,
            cityName = cityName,
            priceRange = priceRange,
            photoUrl = photoUrl,
            address = address,
            isOpen = isOpen,
            isFavorite = true,
            tags = tags,
            brewMethods = brewMethods,
            roasterPhotoUrls = roasterPhotoUrls,
            location = location,
        ),
        location = location,
    )
}
