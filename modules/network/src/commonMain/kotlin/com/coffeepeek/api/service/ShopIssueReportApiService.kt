package com.coffeepeek.api.service

import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.CreateShopIssueReportReq
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body

class ShopIssueReportApiService(private val client: HttpClient) {

    suspend fun submitReport(req: CreateShopIssueReportReq): Result<String?> = requestResult {
        val response = client.postResult("/api/ShopIssueReports") {
            setJsonBody(req)
        }.getOrThrow()
        val apiResponse = response.body<ApiResponse<Unit>>()
        if (!apiResponse.isSuccess) {
            throw ApiException(apiResponse.message)
        }
        apiResponse.entityId
    }
}
