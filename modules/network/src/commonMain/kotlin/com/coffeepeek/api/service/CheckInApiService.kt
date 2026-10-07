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
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

private const val CHECK_INS_PATH = "/api/v1/check-ins"

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

    private suspend fun HttpResponse.checkIn(): CheckInDto {
        val envelope = body<ApiResponse<CheckInDto>>()
        if (!status.isSuccess() || !envelope.isSuccess || envelope.data == null) {
            throw ApiException(envelope.message)
        }
        return envelope.data
    }
}
