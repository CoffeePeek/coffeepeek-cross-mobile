package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopDetails
import kotlinx.coroutines.flow.Flow

/** Favorites capability required by shop browsing; application composition supplies the adapter. */
interface ShopFavorites {
    fun observeIds(): Flow<Result<Set<String>>>
    suspend fun save(shop: ShopDetails): Result<Unit>
    suspend fun remove(shopId: String): Result<Unit>
}
