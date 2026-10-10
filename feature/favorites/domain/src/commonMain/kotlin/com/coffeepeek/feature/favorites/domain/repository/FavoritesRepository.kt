package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import kotlinx.coroutines.flow.Flow

/** Ordered newest-first snapshots. Failures are explicit; cancellation is never a failure. */
interface FavoritesRepository {
    fun observe(): Flow<Result<List<FavoriteShop>>>
    suspend fun read(): Result<List<FavoriteShop>>
    suspend fun save(shop: FavoriteShop): Result<Unit>
    suspend fun remove(shopId: String): Result<Unit>
    suspend fun clear(): Result<Unit>
}
