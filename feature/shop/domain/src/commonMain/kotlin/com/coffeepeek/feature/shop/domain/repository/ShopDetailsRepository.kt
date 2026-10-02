package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopDetails

interface ShopDetailsRepository {
    suspend fun getDetails(shopId: String): Result<ShopDetails>
}
