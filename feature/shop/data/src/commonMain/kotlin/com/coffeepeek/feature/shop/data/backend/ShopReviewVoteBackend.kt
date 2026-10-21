package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopReviewVoteBackend(private val client: HttpClient) {
    suspend fun setHelpful(reviewId: String, helpful: Boolean): Result<ShopHelpfulVote> = requestResult {
        require(reviewId.isNotBlank() && reviewId.none { it == '/' || it == '?' || it == '#' }) {
            "Invalid review ID"
        }
        val path = "/api/CoffeeShopReviews/$reviewId/helpful"
        val response = if (helpful) client.put(path) else client.delete(path)
        val body = response.body<ShopHelpfulResponse>()
        val vote = body.data
        if (!body.isSuccess || vote == null) throw ShopReviewVoteRejected()
        ShopHelpfulVote(vote.isHelpful, vote.helpfulCount)
    }
}

@Serializable
private data class ShopHelpfulResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: ShopHelpfulVoteDto? = null,
)

@Serializable
private data class ShopHelpfulVoteDto(
    @SerialName("isHelpful") val isHelpful: Boolean = false,
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
)

private class ShopReviewVoteRejected : Exception("Review helpful vote was rejected")
