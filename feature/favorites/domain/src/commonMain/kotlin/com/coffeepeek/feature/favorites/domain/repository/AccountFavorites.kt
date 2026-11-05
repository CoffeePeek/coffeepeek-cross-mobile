package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The signed-in account's server favorites; the server is authoritative, memory only caches it. */
interface AccountFavorites {
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
    suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit>

    /** Clears the device's memory only; signing out never deletes server favorites. */
    fun clearCache()
}
