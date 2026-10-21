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
            else if (error.failedBeforeSending()) error
            else ShopReportDeliveryUnconfirmed(error)
        ) },
    )
}

internal class ShopIssueReportRejected(message: String) : Exception(message)

private val PRE_SEND_FAILURES = setOf(
    "ConnectException", "UnknownHostException", "UnresolvedAddressException",
    "ConnectTimeoutException", "NoRouteToHostException",
)

/** The connection never opened, so the server cannot have accepted the report and retrying is safe. */
private fun Throwable.failedBeforeSending(): Boolean =
    generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }.take(8).any { it::class.simpleName in PRE_SEND_FAILURES }
