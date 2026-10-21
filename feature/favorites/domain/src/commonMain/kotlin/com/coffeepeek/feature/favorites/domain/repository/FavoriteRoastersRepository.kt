package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import kotlinx.coroutines.flow.Flow

/** Read/remove port; composition adapts the existing account-owned store and catalogue. */
interface FavoriteRoastersRepository {
    fun observe(): Flow<Result<List<FavoriteRoaster>>>
    suspend fun remove(roasterId: String): Result<Unit>
}
