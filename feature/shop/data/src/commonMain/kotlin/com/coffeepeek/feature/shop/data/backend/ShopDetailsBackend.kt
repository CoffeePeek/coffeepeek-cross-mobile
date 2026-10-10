package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

internal class ShopDetailsBackend(private val client: HttpClient) {
    suspend fun load(shopId: String): Result<ShopDetailsDto> = requestResult {
        require(shopId.isNotBlank() && shopId.none { it == '/' || it == '?' || it == '#' }) {
            "Invalid shop ID"
        }
        val httpResponse = client.get("/api/CoffeeShops/$shopId")
        val response = httpResponse.body<ShopDetailsResponse>()
        if (!httpResponse.status.isSuccess() || !response.isSuccess ||
            response.data == null || response.data.publicAddress.slug.isBlank()) {
            throw ShopDetailsRejected()
        }
        response.data
    }
}

internal class ShopDetailsRejected : Exception("Shop details request was rejected")
