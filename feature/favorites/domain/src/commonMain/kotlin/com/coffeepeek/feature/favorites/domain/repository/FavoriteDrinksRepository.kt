package com.coffeepeek.feature.favorites.domain.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import kotlinx.coroutines.flow.Flow

interface FavoriteDrinksRepository {
    fun observe(): Flow<Result<List<FavoriteDrink>>>
    suspend fun remove(id: String): Result<Unit>
}
