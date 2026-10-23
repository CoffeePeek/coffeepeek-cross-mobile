package com.coffeepeek.feature.contributions.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.network.errorMessage
import com.coffeepeek.core.network.envelopeData
import com.coffeepeek.core.network.ApiEnvelope
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val MODERATION_SHOPS = "/api/ModerationShops"
private const val MODERATION_ROASTERS = "/api/ModerationRoasters"
private const val CHANGE_REQUESTS = "/api/ShopChangeRequests"

/** Plain endpoint calls. Every non-2xx answer becomes a typed failure; nothing here knows user-facing texts. */
internal class ContributionsBackend(private val client: HttpClient) {
    suspend fun shops(status: String, page: Int, size: Int): Result<PageDto<ShopSubmissionDto>> = requestResult {
        mine(MODERATION_SHOPS, status, page, size).envelope()
    }

    suspend fun roasters(status: String, page: Int, size: Int): Result<PageDto<RoasterSubmissionDto>> = requestResult {
        mine(MODERATION_ROASTERS, status, page, size).envelope()
    }

    suspend fun changes(status: String?, page: Int, size: Int, shop: String?, section: String?): Result<PageDto<ChangeRequestDto>> = requestResult {
        val response = client.get("$CHANGE_REQUESTS/mine") {
            expectSuccess = false
            parameter("page", page)
            parameter("pageSize", size)
            status?.let { parameter("status", it) }
            shop?.let { parameter("shop", it) }
            section?.let { parameter("section", it) }
        }
        response.requireSuccess()
        response.envelope()
    }

    suspend fun change(id: String): Result<ChangeRequestDto> = requestResult {
        val response = client.get("$CHANGE_REQUESTS/$id") { expectSuccess = false }
        response.requireSuccess()
        response.envelope()
    }

    suspend fun createShop(request: CreateShopRequest): Result<Unit> = requestResult {
        val response = client.post(MODERATION_SHOPS) { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
    }

    suspend fun submitRoaster(request: CreateRoasterRequest): Result<Pair<RoasterSubmittedDto, String?>> = requestResult {
        val response = client.post(MODERATION_ROASTERS) { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
        val envelope = response.body<ApiEnvelope<RoasterSubmittedDto>>()
        if (!envelope.isSuccess) throw ContributionFailure(FailureKind.Other, envelope.message.clean())
        checkNotNull(envelope.data) { "Missing response data" } to envelope.message.clean()
    }

    suspend fun createChange(request: CreateChangeRequest): Result<ChangeRequestDto> = requestResult {
        val response = client.post(CHANGE_REQUESTS) { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
        response.envelope()
    }

    suspend fun updateChange(id: String, request: UpdateChangeRequest): Result<ChangeRequestDto> = requestResult {
        val response = client.put("$CHANGE_REQUESTS/$id") { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
        response.envelope()
    }

    private suspend fun mine(path: String, status: String, page: Int, size: Int): HttpResponse {
        val response = client.get("$path/mine") {
            expectSuccess = false
            parameter("page", page)
            parameter("pageSize", size)
            parameter("status", status)
        }
        response.requireSuccess()
        return response
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        throw ContributionFailure(
            when (status) {
                HttpStatusCode.Unauthorized -> FailureKind.SignInRequired
                HttpStatusCode.Forbidden -> FailureKind.Forbidden
                HttpStatusCode.NotFound -> FailureKind.NotFound
                else -> FailureKind.Other
            },
            errorMessage(),
        )
    }

    private suspend inline fun <reified T> HttpResponse.envelope(): T = envelopeData { ContributionFailure(FailureKind.Other, it) }

    private fun String?.clean(): String? = this?.takeIf(String::isNotBlank)
}
