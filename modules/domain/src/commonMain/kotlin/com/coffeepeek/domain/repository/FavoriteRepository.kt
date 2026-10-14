package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails

interface FavoriteRepository {
    suspend fun getFavoriteIds(): Set<String>
    suspend fun isFavorite(shopId: String): Boolean
    suspend fun getFavorites(): Result<List<CoffeeShopDetails>>
    suspend fun addFavorite(shop: CoffeeShop, address: String? = null): Result<Unit>
    suspend fun removeFavorite(shopId: String): Result<Unit>
    suspend fun clearAll()
    suspend fun clearForUser(userId: String?) { clearAll() }
}
