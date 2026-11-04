package com.coffeepeek.domain.feature.favorites.api

import com.coffeepeek.domain.model.PublicAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class FavoriteKind { CoffeeShop, Roaster, Drink }

/** Stable membership identity, independent of optional catalogue/display metadata. */
data class FavoriteTarget(val kind: FavoriteKind, val slug: String)

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
    /** Known memberships can be projected before row metadata finishes loading. */
    fun observeMemberships(): Flow<Result<Set<FavoriteTarget>>> = observeFavorites().map { result ->
        result.map { entries -> entries.mapNotNull { entry -> entry.slug?.let { FavoriteTarget(entry.kind, it) } }.toSet() }
    }
    /** Null means the active account has no confirmed answer for this target yet. */
    fun cachedMembership(target: FavoriteTarget): Boolean? = null
    /** Applies a confirmed server response to this device's projection; never writes a favorite. */
    fun applyConfirmedMembership(expectedAccount: String, expectedGeneration: Long, target: FavoriteTarget, favorite: Boolean): Boolean = false
    suspend fun getFavorites(refresh: Boolean = false): Result<List<FavoriteEntry>>
    suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit>
    suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
        val address = entry.address ?: return Result.failure(IllegalArgumentException("У объекта нет публичного адреса"))
        return setFavorite(entry.kind, address, favorite)
    }
    /** Clears the device's memory only; signing out never deletes server favorites. */
    fun clearCache()
}
