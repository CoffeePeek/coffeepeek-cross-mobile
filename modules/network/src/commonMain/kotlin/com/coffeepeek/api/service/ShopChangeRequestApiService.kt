package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.CreateShopChangeRequestBody
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.request.ShopChangeRequestDto
import com.coffeepeek.api.model.request.ShopChangeRequestPageDto
import com.coffeepeek.api.model.request.ShopChangeSectionDto
import com.coffeepeek.api.model.request.UpdateShopChangeRequestBody
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.getResult
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.putResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class ShopChangeRequestApiService(private val client: HttpClient) {

    suspend fun create(body: CreateShopChangeRequestBody): Result<ShopChangeRequestDto> = runCatching {
        val response = client.postResult("/api/ShopChangeRequests") {
            setJsonBody(body)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<ShopChangeRequestDto>>()
        if (!response.status.isSuccess() || !apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun getMine(
        page: Int,
        pageSize: Int,
        status: ModerationStatusDto? = null,
        shopId: String? = null,
        section: ShopChangeSectionDto? = null,
    ): Result<ShopChangeRequestPageDto> = getPage(
        path = "/api/ShopChangeRequests/mine",
        page = page,
        pageSize = pageSize,
        status = status,
        shopId = shopId,
        section = section,
    )

    suspend fun getById(id: String): Result<ShopChangeRequestDto> = runCatching {
        val response = client.getResult("/api/ShopChangeRequests/$id").getOrThrow()
        val apiResponse = response.body<ApiResponse<ShopChangeRequestDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    suspend fun update(id: String, body: UpdateShopChangeRequestBody): Result<ShopChangeRequestDto> = runCatching {
        val response = client.putResult("/api/ShopChangeRequests/$id") {
            setJsonBody(body)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<ShopChangeRequestDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }

    private suspend fun getPage(
        path: String,
        page: Int,
        pageSize: Int,
        status: ModerationStatusDto?,
        shopId: String?,
        section: ShopChangeSectionDto?,
    ): Result<ShopChangeRequestPageDto> = runCatching {
        val response = client.getResult(path) {
            parameter("page", page)
            parameter("pageSize", pageSize)
            status?.let { parameter("status", it.name) }
            shopId?.let { parameter("shop", it) }
            section?.let { parameter("section", it.name) }
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<ShopChangeRequestPageDto>>()
        if (!apiResponse.isSuccess || apiResponse.data == null) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.data
    }
}
