package com.coffeepeek.admin.feature.favorites.api

import com.coffeepeek.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Server favorites for the active account, shared by roaster and shop presentation. */
interface RoasterFavorites {
    fun observeFavorites(): Flow<List<CatalogItem>>
    fun observeErrors(): Flow<String?> = flowOf(null)
    suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit>

    /** Fail closed when a legacy implementation cannot bind a mutation to its original owner. */
    suspend fun removeForAccount(roaster: CatalogItem, expectedUserId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Account-bound removal is not supported"))
}

// Catalog keys can be GUIDs; public slugs also identify roasters opened from their detail page.
val CatalogItem.roasterFavoriteId: String get() = address?.slug ?: id
