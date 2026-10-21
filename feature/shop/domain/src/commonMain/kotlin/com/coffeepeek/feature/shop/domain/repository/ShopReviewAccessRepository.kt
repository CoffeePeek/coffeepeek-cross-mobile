package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopReviewAccess

interface ShopReviewAccessRepository {
    suspend fun getAccess(shopId: String): Result<ShopReviewAccess>
}
