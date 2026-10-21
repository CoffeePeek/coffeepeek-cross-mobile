package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput

interface ShopReviewWriteRepository {
    suspend fun create(input: ShopReviewCreateInput): Result<Unit>
    suspend fun update(input: ShopReviewUpdateInput): Result<Unit>
}
