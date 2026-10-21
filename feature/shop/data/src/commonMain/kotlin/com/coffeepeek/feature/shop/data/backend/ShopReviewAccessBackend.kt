package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopReviewAccess
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopReviewAccessBackend(private val client: HttpClient) {
    suspend fun getAccess(shopId: String): Result<ShopReviewAccess> = requestResult {
        require(shopId.isNotBlank()) { "Invalid shop ID" }
        val response = client.get("/api/CoffeeShopReviews/can-create") {
            parameter("shopId", shopId)
        }.body<ReviewAccessResponse>()
        val access = response.data
        if (!response.isSuccess || access == null) throw ShopReviewAccessRejected()
        ShopReviewAccess(access.canCreate, access.reviewId?.takeIf(String::isNotBlank))
    }
}

@Serializable
private data class ReviewAccessResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: ReviewAccessDto? = null,
)

@Serializable
private data class ReviewAccessDto(
    @SerialName("canCreate") val canCreate: Boolean = false,
    @SerialName("reviewId") val reviewId: String? = null,
)

private class ShopReviewAccessRejected : Exception("Review access request was rejected")
