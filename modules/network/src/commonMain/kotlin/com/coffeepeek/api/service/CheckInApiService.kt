package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.CreateCheckInReq
import com.coffeepeek.api.model.response.CreateCheckInResponseDto
import com.coffeepeek.api.model.response.GetUserCheckInsResponseDto
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.getResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.isSuccess

class CheckInApiService(private val client: HttpClient) {

    suspend fun createCheckIn(req: CreateCheckInReq): Result<Unit> = runCatching {
        val response = client.post("/api/CheckIns") {
            setJsonBody(req)
        }
        val apiResponse = runCatching {
            response.body<ApiResponse<CreateCheckInResponseDto>>()
        }.getOrNull()

        when {
            apiResponse != null && !apiResponse.isSuccess -> {
                throw ApiException(
                    apiResponse.message.orEmpty().ifBlank { "Не удалось сохранить чек-ин" },
                )
            }
            !response.status.isSuccess() -> {
                throw ApiException(
                    apiResponse?.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось сохранить чек-ин (${response.status.value})",
                )
            }
        }
    }

    suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<GetUserCheckInsResponseDto> = runCatching {
        val response = client.getResult("/api/CheckIns") {
            header("X-Page-Number", page)
            header("X-Page-Size", pageSize)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<GetUserCheckInsResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<GetUserCheckInsResponseDto> = runCatching {
        val response = client.getResult("/api/CheckIns") {
            parameter("from", from)
            parameter("to", to)
            header("X-Page-Number", 1)
            header("X-Page-Size", pageSize)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<GetUserCheckInsResponseDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }
}
