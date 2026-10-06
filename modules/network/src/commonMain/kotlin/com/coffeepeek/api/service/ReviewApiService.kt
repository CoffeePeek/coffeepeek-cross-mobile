package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.SendReviewReq
import com.coffeepeek.api.model.request.UpdateReviewReq
import com.coffeepeek.api.model.response.CreateEntityResponseDto
import com.coffeepeek.api.model.response.GetReviewsByUserIdResponseDto
import com.coffeepeek.api.model.response.MyModerationReviewsPageDto
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.response.ReviewHelpfulResponseDto
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.deleteResult
import com.coffeepeek.api.utils.getResult
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.putResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewReportRequest(val text: String)

@Serializable
internal data class ReviewReportResponse(val id: String)

class ReviewApiService(private val client: HttpClient) {

    suspend fun submitReviewReport(reviewId: String, text: String): Result<String> = runCatching {
        val trimmed = text.trim()
        require(trimmed.length in 1..2000) { "Опишите проблему: от 1 до 2000 символов" }
        val response = client.postResult("/api/CoffeeShopReviews/$reviewId/reports") {
            expectSuccess = false
            setJsonBody(ReviewReportRequest(trimmed))
        }.getOrThrow()
        when (response.status) {
            HttpStatusCode.Unauthorized -> throw ApiException("Войдите в аккаунт, чтобы отправить жалобу")
            HttpStatusCode.NotFound -> throw ApiException("Отзыв удалён или больше недоступен")
            HttpStatusCode.TooManyRequests -> throw ApiException("Слишком много отправок. Попробуйте позже")
        }
        if (response.status != HttpStatusCode.Created) {
            throw ApiException("Не удалось отправить жалобу. Попробуйте ещё раз")
        }
        val result = response.body<ApiResponse<ReviewReportResponse>>()
        val id = result.data?.id
        if (!result.isSuccess || id.isNullOrBlank()) throw ApiException(result.message)
        id
    }

    suspend fun createReview(req: SendReviewReq): Result<Unit> = runCatching {
        val response = client.postResult("/api/ModerationReviews") {
            setJsonBody(req)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<CreateEntityResponseDto?>>()
        if (!apiResponse.isSuccess) {
            throw ApiException(apiResponse.message)
        }
    }

    suspend fun updateReview(reviewId: String, req: UpdateReviewReq): Result<Unit> = runCatching {
        val response = client.putResult("/api/ModerationReviews/$reviewId") {
            setJsonBody(req)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<Unit>>()
        if (!apiResponse.isSuccess) {
            throw ApiException(apiResponse.message)
        }
    }

    /** PUT to mark helpful, DELETE to remove the vote. Both idempotent; returns fresh count/state. */
    suspend fun setReviewHelpful(reviewId: String, helpful: Boolean): Result<ReviewHelpfulResponseDto> = runCatching {
        val path = "/api/CoffeeShopReviews/$reviewId/helpful"
        val response = (if (helpful) client.putResult(path) else client.deleteResult(path)).getOrThrow()
        val apiResponse = response.body<ApiResponse<ReviewHelpfulResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun getUserReviews(userId: String, page: Int, pageSize: Int): Result<GetReviewsByUserIdResponseDto> =
        runCatching {
            val response = client.getResult("/api/users/$userId/reviews") {
                parameter("pageNumber", page)
                parameter("pageSize", pageSize)
            }.getOrThrow()
            val apiResponse = response.body<ApiResponse<GetReviewsByUserIdResponseDto>>()
            if (!apiResponse.isSuccess || apiResponse.data == null) {
                throw ApiException(apiResponse.message)
            }
            apiResponse.data
        }

    suspend fun getMyModerationReviews(
        status: ModerationStatusDto,
        page: Int,
        pageSize: Int,
    ): Result<MyModerationReviewsPageDto> = runCatching {
        val response = client.getResult("/api/ModerationReviews/mine") {
            parameter("page", page)
            parameter("pageSize", pageSize)
            parameter("status", status.name)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<MyModerationReviewsPageDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }
}
