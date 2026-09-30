package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopOverview

interface ShopOverviewRepository {
    suspend fun getOverview(shopId: String): Result<ShopOverview>
}
