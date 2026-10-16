package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopRating
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopReviewWriteBackend(private val client: HttpClient) {
    suspend fun create(
        shopId: String,
        header: String,
        comment: String,
        rating: ShopRating,
        photos: List<UploadedShopPhoto>,
    ): Result<Unit> = requestResult {
        val response = client.post("/api/ModerationReviews") {
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(shopId, header, comment, rating.toRequest(),
                photos.takeIf { it.isNotEmpty() }))
        }.body<WriteResponse>()
        if (!response.isSuccess) throw ShopReviewWriteRejected()
    }

    suspend fun update(
        reviewId: String,
        header: String,
        comment: String,
        rating: ShopRating,
    ): Result<Unit> = requestResult {
        require(reviewId.isNotBlank() && reviewId.none { it == '/' || it == '?' || it == '#' }) {
            "Invalid review ID"
        }
        val response = client.put("/api/ModerationReviews/$reviewId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateReviewRequest(reviewId, header, comment, rating.toRequest()))
        }.body<WriteResponse>()
        if (!response.isSuccess) throw ShopReviewWriteRejected()
    }
}

@Serializable
private data class CreateReviewRequest(
    @SerialName("shopId") val shopId: String,
    @SerialName("header") val header: String,
    @SerialName("comment") val comment: String,
    @SerialName("rating") val rating: RatingRequest,
    @SerialName("photos") val photos: List<UploadedShopPhoto>?,
)

@Serializable
private data class UpdateReviewRequest(
    @SerialName("reviewId") val reviewId: String,
    @SerialName("header") val header: String,
    @SerialName("comment") val comment: String,
    @SerialName("rating") val rating: RatingRequest,
)

@Serializable
private data class RatingRequest(
    @SerialName("place") val place: Int,
    @SerialName("service") val service: Int,
    @SerialName("coffee") val coffee: Int,
)

@Serializable
private data class WriteResponse(@SerialName("isSuccess") val isSuccess: Boolean = false)

private fun ShopRating.toRequest() = RatingRequest(place, service, coffee)

private class ShopReviewWriteRejected : Exception("Review write request was rejected")
