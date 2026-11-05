package com.coffeepeek.feature.contentreport.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.contentreport.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.json.*

internal class ContentReportBackend(private val client: HttpClient) {
    suspend fun submit(target: ContentReportTarget, reason: String): Result<Unit> {
        if (target.id.isBlank() || !validReportReason(reason)) return Result.failure(ContentReportFailure(ContentReportProblem.InvalidInput))
        return requestResult {
            val path = if (target.kind == ContentReportKind.CheckIn) "/api/v1/check-ins" else "/api/CoffeeShopReviews"
            val response = client.post(path) {
                url { appendPathSegments(target.id, "reports", encodeSlash = true) }
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(ContentReportRequest(reason.trim()))
            }
            response.verify(target.kind)
        }.fold(onSuccess = { Result.success(it) }, onFailure = { error ->
            Result.failure(error as? ContentReportFailure ?: ContentReportFailure(ContentReportProblem.Unconfirmed))
        })
    }
}

private suspend fun HttpResponse.verify(kind: ContentReportKind) {
    if (status.value in 400..499) throw ContentReportFailure(when (status) {
        HttpStatusCode.Unauthorized -> ContentReportProblem.SignInRequired
        HttpStatusCode.Forbidden -> ContentReportProblem.Forbidden
        HttpStatusCode.NotFound -> ContentReportProblem.NotFound
        HttpStatusCode.TooManyRequests -> ContentReportProblem.RateLimited
        else -> ContentReportProblem.Rejected
    })
    if (status != HttpStatusCode.Created) throw ContentReportFailure(ContentReportProblem.Unconfirmed)
    val envelope = body<ContentReportResponse>()
    if (!envelope.isSuccess) throw ContentReportFailure(ContentReportProblem.Rejected, envelope.message?.takeIf(String::isNotBlank))
    val data = envelope.data
    val receipt = if (kind == ContentReportKind.Review) (data as? JsonObject)?.get("id") as? JsonPrimitive else null
    if (data == null || data is JsonNull || kind == ContentReportKind.Review &&
        (receipt?.isString != true || receipt.contentOrNull.isNullOrBlank())) {
        throw ContentReportFailure(ContentReportProblem.Unconfirmed)
    }
}
