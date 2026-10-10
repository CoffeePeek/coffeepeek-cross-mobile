package com.coffeepeek.feature.shopreport.data.backend

import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

internal class ShopIssueReportBackend(private val client: HttpClient) {
    suspend fun submit(request: CreateShopIssueReportRequest): Result<Unit> = requestResult {
        val response = client.post("/api/ShopIssueReports") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val body = response.body<ShopIssueReportResponse>()
        if (!response.status.isSuccess() || !body.isSuccess) {
            throw ShopIssueReportRejected(body.message)
        }
    }
}

internal class ShopIssueReportRejected(message: String) : Exception(message)
