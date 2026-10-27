package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import kotlinx.coroutines.flow.Flow

/** The signed-in account's server favorites; the server is authoritative, memory only caches it. */
interface AccountFavorites {
    fun observeFavorites(): Flow<Result<List<FavoriteEntry>>>
    suspend fun getFavorites(refresh: Boolean = false): Result<List<FavoriteEntry>>
    suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit>

    /** Clears the device's memory only; signing out never deletes server favorites. */
    fun clearCache()
}
