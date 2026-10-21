package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.data.mapper.ShopFileUrlResolver
import com.coffeepeek.feature.shop.data.mapper.toDomain
import com.coffeepeek.feature.shop.domain.model.ShopReview
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The existing user-reviews endpoint has no single-review lookup. */
internal class ShopUserReviewsBackend(
    private val client: HttpClient,
    private val files: ShopFileUrlResolver,
) {
    suspend fun findForEdit(userId: String, reviewId: String): Result<ShopReview?> = requestResult {
        require(userId.isSafePathSegment() && reviewId.isSafePathSegment()) { "Invalid review lookup ID" }
        var page = 1
        var lastPage = 1
        do {
            val response = client.get("/api/users/$userId/reviews") {
                parameter("pageNumber", page)
                parameter("pageSize", 100)
            }.body<UserReviewsResponse>()
            val data = response.data
            if (!response.isSuccess || data == null) throw ShopUserReviewsRejected()
            data.reviews.firstOrNull { it.id == reviewId && it.userId == userId }
                ?.toDomain(files)?.let { return@requestResult it }
            lastPage = data.totalPages.coerceAtLeast(1)
            page++
        } while (page <= lastPage)
        null
    }
}

private fun String.isSafePathSegment(): Boolean = isNotBlank() &&
    none { it == '/' || it == '?' || it == '#' || it == '%' || it.isWhitespace() }

@Serializable
private data class UserReviewsResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: UserReviewsPage? = null,
)

@Serializable
private data class UserReviewsPage(
    @SerialName("reviewDtos") val reviews: List<ShopReviewDto> = emptyList(),
    @SerialName("totalPages") val totalPages: Int = 1,
)

private class ShopUserReviewsRejected : Exception("User review lookup was rejected")
