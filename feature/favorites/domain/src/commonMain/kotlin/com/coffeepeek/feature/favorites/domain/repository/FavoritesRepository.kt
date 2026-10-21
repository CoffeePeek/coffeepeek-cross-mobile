package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ordered newest-first snapshots. Failures are explicit; cancellation is never a failure. */
interface FavoritesRepository {
    fun observe(): Flow<Result<List<FavoriteShop>>>
    /** Membership consumers need not hydrate every saved shop card. */
    fun observeIds(): Flow<Result<Set<String>>> = observe().map { result -> result.map { rows -> rows.mapTo(mutableSetOf()) { it.id } } }
    suspend fun read(): Result<List<FavoriteShop>>
    suspend fun save(shop: FavoriteShop): Result<Unit>
    suspend fun remove(shopId: String): Result<Unit>
    suspend fun clear(): Result<Unit>
}
