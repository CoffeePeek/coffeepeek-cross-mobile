package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.CheckInVisibilityReq
import com.coffeepeek.api.model.request.CreateCheckInReq
import com.coffeepeek.api.model.request.UpdateCheckInReq
import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.api.model.response.GetUserCheckInsResponseDto
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

private const val CHECK_INS_PATH = "/api/v1/check-ins"

@Serializable
data class CheckInHelpfulDto(val isHelpful: Boolean, val helpfulCount: Int)
@Serializable
private data class CheckInReportReq(val text: String)

class CheckInApiService(private val client: HttpClient) {
    suspend fun createCheckIn(req: CreateCheckInReq): Result<CheckInDto> = runCatching {
        client.post(CHECK_INS_PATH) { setJsonBody(req) }.checkIn()
    }

    suspend fun getMyCheckIns(
        page: Int,
        pageSize: Int,
        from: String? = null,
        to: String? = null,
    ): Result<GetUserCheckInsResponseDto> = runCatching {
        val response = client.get("$CHECK_INS_PATH/mine") {
            parameter("pageNumber", page.coerceAtLeast(1))
            parameter("pageSize", pageSize.coerceIn(1, 100))
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }
        val envelope = response.body<ApiResponse<GetUserCheckInsResponseDto>>()
        if (!response.status.isSuccess() || !envelope.isSuccess || envelope.data == null) {
            throw ApiException(envelope.message)
        }
        envelope.data
    }

    suspend fun updateCheckIn(id: String, req: UpdateCheckInReq): Result<CheckInDto> = runCatching {
        client.put("$CHECK_INS_PATH/$id") { setJsonBody(req) }.checkIn()
    }

    suspend fun setVisibility(id: String, req: CheckInVisibilityReq): Result<CheckInDto> = runCatching {
        client.put("$CHECK_INS_PATH/$id/visibility") { setJsonBody(req) }.checkIn()
    }

    suspend fun setHelpful(id: String, helpful: Boolean): Result<CheckInHelpfulDto> = runCatching {
        val path = "$CHECK_INS_PATH/$id/helpful"
        val response = if (helpful) client.put(path) { expectSuccess = false }
            else client.delete(path) { expectSuccess = false }
        response.requireSuccess()
        val envelope = response.body<ApiResponse<CheckInHelpfulDto>>()
        if (!envelope.isSuccess || envelope.data == null) throw ApiException(envelope.message)
        envelope.data
    }

    suspend fun report(id: String, text: String): Result<Unit> = runCatching {
        require(text.trim().length in 1..2000) { "Опишите проблему: от 1 до 2000 символов" }
        val response = client.post("$CHECK_INS_PATH/$id/reports") {
            expectSuccess = false
            setJsonBody(CheckInReportReq(text.trim()))
        }
        response.requireSuccess()
        val envelope = response.body<ApiResponse<kotlinx.serialization.json.JsonElement>>()
        if (response.status != HttpStatusCode.Created || !envelope.isSuccess || envelope.data == null) {
            throw ApiException(envelope.message)
        }
    }

    private fun HttpResponse.requireSuccess() {
        if (!status.isSuccess()) throw ApiException(when (status) {
            HttpStatusCode.Unauthorized -> "Войдите в аккаунт"
            HttpStatusCode.Forbidden -> "Действие недоступно для этого чек-ина"
            HttpStatusCode.NotFound -> "Чек-ин удалён или больше недоступен"
            else -> "Не удалось выполнить действие. Попробуйте ещё раз"
        })
    }

    private suspend fun HttpResponse.checkIn(): CheckInDto {
        val envelope = body<ApiResponse<CheckInDto>>()
        if (!status.isSuccess() || !envelope.isSuccess || envelope.data == null) {
            throw ApiException(envelope.message)
        }
        return envelope.data
    }
}
