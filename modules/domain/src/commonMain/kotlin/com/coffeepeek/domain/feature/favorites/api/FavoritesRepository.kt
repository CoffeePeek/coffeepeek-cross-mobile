package com.coffeepeek.domain.feature.favorites.api

import com.coffeepeek.domain.model.PublicAddress
import kotlinx.coroutines.flow.Flow

enum class FavoriteKind { CoffeeShop, Roaster, Drink }

data class FavoriteEntry(
    val kind: FavoriteKind,
    val address: PublicAddress? = null,
    val drinkSlug: String? = null,
    val name: String = "",
    val coverPhotoUrl: String? = null,
    val createdAtUtc: String = "",
    /** Local display identity only, for addressless entries without a contracted target ID. */
    val occurrence: Int = 0,
) {
    val slug: String? get() = if (kind == FavoriteKind.Drink) drinkSlug else address?.slug
    val id: String get() = "${kind.name}:${slug ?: "$createdAtUtc:$name:$occurrence"}"
}

/** The signed-in account's server favorites. Public profiles use a separate contract. */
interface FavoritesRepository {
    fun observeFavorites(): Flow<Result<List<FavoriteEntry>>>
    suspend fun getFavorites(refresh: Boolean = false): Result<List<FavoriteEntry>>
    suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit>
    suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
        val address = entry.address ?: return Result.failure(IllegalArgumentException("У объекта нет публичного адреса"))
        return setFavorite(entry.kind, address, favorite)
    }
    /** Clears the device's memory only; signing out never deletes server favorites. */
    fun clearCache()
}
