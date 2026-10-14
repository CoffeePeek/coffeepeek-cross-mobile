package com.coffeepeek.api.service

import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.api.model.response.ConsumedDrinkOptionDto

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.CreateShopReq
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.request.ShopSearchRequestDto
import com.coffeepeek.api.model.response.shop.MyModerationShopsPageDto
import com.coffeepeek.api.model.response.shop.CatalogItemDto
import com.coffeepeek.api.model.response.shop.CityItemDto
import com.coffeepeek.api.model.response.shop.CoffeeShopDetailsDto
import com.coffeepeek.api.model.response.shop.GetDrinksResponseDto
import com.coffeepeek.api.model.response.shop.CoffeeDrinkDefinitionDto
import com.coffeepeek.api.model.response.shop.GetShopsInBoundsResponseDto
import com.coffeepeek.api.model.response.shop.GetShopsResponseDto
import com.coffeepeek.api.model.response.shop.ShopSearchResponseDto
import com.coffeepeek.api.utils.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ShopApiService(private val client: HttpClient) {

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun searchShops(
        query: String? = null,
        cityId: String? = null,
        type: String? = null,
        roasterIds: List<String>? = null,
        equipmentIds: List<String>? = null,
        beanIds: List<String>? = null,
        brewMethodIds: List<String>? = null,
        tagIds: List<String>? = null,
        priceRange: String? = null,
        minRating: Double? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<GetShopsResponseDto> = requestResult {
        val response = client.get("/api/CoffeeShops") {
            query?.let { parameter("q", it) }
            cityId?.let { parameter("city", it) }
            type?.let { parameter("type", it) }
            roasterIds?.forEach { parameter("roasters", it) }
            equipmentIds?.forEach { parameter("equipments", it) }
            beanIds?.forEach { parameter("beans", it) }
            brewMethodIds?.forEach { parameter("brewMethods", it) }
            tagIds?.forEach { parameter("tags", it) }
            priceRange?.let { parameter("priceRange", it) }
            minRating?.let { parameter("minRating", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
        val apiResponse = response.body<ApiResponse<GetShopsResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun searchNearbyShops(request: ShopSearchRequestDto): Result<ShopSearchResponseDto> = requestResult {
        val origin = request.filters.origin
        require(origin.latitude.isFinite() && origin.latitude in -90.0..90.0)
        require(origin.longitude.isFinite() && origin.longitude in -180.0..180.0)
        require(request.filters.radiusKm > 0 && request.page > 0 && request.pageSize in 1..100)
        val response = client.post("/api/v1/coffee-shops/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val apiResponse = response.body<ApiResponse<ShopSearchResponseDto>>()
        if (!response.status.isSuccess() || !apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun getShopDetails(id: String): Result<CoffeeShopDetailsDto> = requestResult {
        val response = client.get("/api/CoffeeShops/$id")
        val apiResponse = response.body<ApiResponse<CoffeeShopDetailsDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOptionDto>> = requestResult {
        val response = client.get("/api/catalogs/drinks")
        val apiResponse = response.body<ApiResponse<List<ConsumedDrinkOptionDto>>>()
        if (!response.status.isSuccess() || !apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinitionDto>> = requestResult {
        val response = client.get("/api/menu/drinks")
        val apiResponse = response.body<ApiResponse<GetDrinksResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data.drinks
    }

    // ── Create ────────────────────────────────────────────────────────────────

    suspend fun createShop(req: CreateShopReq): Result<Unit> = requestResult {
        val response = client.post("/api/ModerationShops") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!response.status.isSuccess()) {
            val apiResponse = requestResult { response.body<ApiResponse<Unit>>() }.getOrNull()
            throw ApiException(apiResponse?.message ?: "Ошибка создания кофейни (${response.status.value})")
        }
    }

    suspend fun getMyModerationShops(
        status: ModerationStatusDto,
        page: Int,
        pageSize: Int,
    ): Result<MyModerationShopsPageDto> = requestResult {
        val response = client.get("/api/ModerationShops/mine") {
            parameter("page", page)
            parameter("pageSize", pageSize)
            parameter("status", status.name)
        }
        val apiResponse = response.body<ApiResponse<MyModerationShopsPageDto>>()
        if (!response.status.isSuccess() || !apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    // ── Catalogs ──────────────────────────────────────────────────────────────

    suspend fun getCities(): Result<List<CityItemDto>> = requestResult {
        val response = client.get("/api/Catalogs/cities")
        val apiResponse = response.body<ApiResponse<List<CityItemDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getBeans(): Result<List<CatalogItemDto>> = requestResult {
        val response = client.get("/api/Catalogs/beans")
        val apiResponse = response.body<ApiResponse<List<CatalogItemDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getEquipment(): Result<List<CatalogItemDto>> = requestResult {
        val response = client.get("/api/Catalogs/equipments")
        val apiResponse = response.body<ApiResponse<List<CatalogItemDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getBrewMethods(): Result<List<CatalogItemDto>> = requestResult {
        val response = client.get("/api/Catalogs/brew-methods")
        val apiResponse = response.body<ApiResponse<List<CatalogItemDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getShopTags(): Result<List<CatalogItemDto>> = requestResult {
        val response = client.get("/api/Catalogs/shop-tags")
        val apiResponse = response.body<ApiResponse<List<CatalogItemDto>>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }

    suspend fun getShopsInBounds(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        zoom: Int,
        cityId: String? = null,
        type: String? = null,
        roasterIds: List<String>? = null,
        equipmentIds: List<String>? = null,
        beanIds: List<String>? = null,
        brewMethodIds: List<String>? = null,
        tagIds: List<String>? = null,
        priceRange: String? = null,
        minRating: Double? = null,
    ): Result<GetShopsInBoundsResponseDto> = requestResult {
        val response = client.get("/api/Map") {
            parameter("minLat", minLat)
            parameter("minLon", minLon)
            parameter("maxLat", maxLat)
            parameter("maxLon", maxLon)
            parameter("zoom", zoom)
            cityId?.let { parameter("city", it) }
            type?.let { parameter("type", it) }
            roasterIds?.forEach { parameter("roasters", it) }
            equipmentIds?.forEach { parameter("equipments", it) }
            beanIds?.forEach { parameter("beans", it) }
            brewMethodIds?.forEach { parameter("brewMethods", it) }
            tagIds?.forEach { parameter("tags", it) }
            priceRange?.let { parameter("priceRange", it) }
            minRating?.let { parameter("minRating", it) }
        }
        val apiResponse = response.body<ApiResponse<GetShopsInBoundsResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) throw ApiException(apiResponse.message)
        apiResponse.data
    }
}
