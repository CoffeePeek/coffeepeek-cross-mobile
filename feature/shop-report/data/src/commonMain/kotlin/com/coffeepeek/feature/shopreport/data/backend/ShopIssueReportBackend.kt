package com.coffeepeek.feature.shopreport.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shopreport.domain.model.ShopReportDeliveryUnconfirmed
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
        if (response.status.value in 400..499) throw ShopIssueReportRejected("Report rejected")
        if (!response.status.isSuccess()) throw ShopReportDeliveryUnconfirmed()
        val body = response.body<ShopIssueReportResponse>()
        if (!response.status.isSuccess() || !body.isSuccess) {
            throw ShopIssueReportRejected(body.message)
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { error -> Result.failure(
            if (error is ShopIssueReportRejected || error is ShopReportDeliveryUnconfirmed) error
            else ShopReportDeliveryUnconfirmed(error)
        ) },
    )
}

internal class ShopIssueReportRejected(message: String) : Exception(message)
