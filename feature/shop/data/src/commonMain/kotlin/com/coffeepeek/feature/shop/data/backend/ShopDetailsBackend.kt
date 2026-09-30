package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class ShopDetailsBackend(private val client: HttpClient) {
    suspend fun load(shopId: String): Result<ShopDetailsData> = requestResult {
        require(shopId.isNotBlank() && shopId.none { it == '/' || it == '?' || it == '#' }) {
            "Invalid shop ID"
        }
        val response = client.get("/api/CoffeeShops/$shopId").body<ShopDetailsResponse>()
        if (!response.isSuccess || response.data == null) {
            throw ShopDetailsRejected()
        }
        response.data
    }
}

internal class ShopDetailsRejected : Exception("Shop details request was rejected")
