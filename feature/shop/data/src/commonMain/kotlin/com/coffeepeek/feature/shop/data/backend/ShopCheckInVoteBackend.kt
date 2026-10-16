package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class ShopCheckInVoteBackend(private val client: HttpClient) {
    suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> = requestResult {
        require(checkInId.isNotBlank() && checkInId.none { it == '/' || it == '?' || it == '#' }) {
            "Invalid check-in ID"
        }
        val path = "/api/v1/check-ins/$checkInId/helpful"
        val response = if (helpful) client.put(path) { expectSuccess = false }
            else client.delete(path) { expectSuccess = false }
        check(response.status.isSuccess()) { "Check-in vote request was rejected" }
        val body = response.body<ShopCheckInVoteResponse>()
        val vote = body.data
        check(body.isSuccess && vote != null && vote.helpfulCount >= 0) { "Check-in vote response was rejected" }
        ShopHelpfulVote(vote.isHelpful, vote.helpfulCount)
    }
}

@Serializable
private data class ShopCheckInVoteResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: ShopCheckInVoteDto? = null,
)

@Serializable
private data class ShopCheckInVoteDto(
    @SerialName("isHelpful") val isHelpful: Boolean,
    @SerialName("helpfulCount") val helpfulCount: Int,
)
