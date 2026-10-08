package com.coffeepeek.admin.feature.favorites.api

import com.coffeepeek.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow

/** Local favorites for the active account, shared by roaster and shop presentation. */
interface RoasterFavorites {
    fun observeFavorites(): Flow<List<CatalogItem>>
    suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit>
}

// Catalog keys can be GUIDs; public slugs also identify roasters opened from their detail page.
val CatalogItem.roasterFavoriteId: String get() = address?.slug ?: id
