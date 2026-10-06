package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption

interface ShopCheckInRepository {
    suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>>
    suspend fun create(input: ShopCheckInCreateInput): Result<Unit>
}
