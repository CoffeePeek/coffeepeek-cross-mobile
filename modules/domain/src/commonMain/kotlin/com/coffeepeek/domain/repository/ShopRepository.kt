package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.ConsumedDrinkOption

import com.coffeepeek.domain.model.CoffeeDrinkDefinition
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CreateShopInput
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.ShopCatalogs
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopSubmission

interface ShopRepository {
    suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>>
    suspend fun getShopDetails(id: String): Result<CoffeeShopDetails>
    suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters = ShopFilters()): Result<MapContent>
    suspend fun getCatalogs(): Result<ShopCatalogs>
    suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>>
    suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>>
    suspend fun createShop(input: CreateShopInput): Result<Unit>
    suspend fun getMyShopSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<ShopSubmission>>
}
