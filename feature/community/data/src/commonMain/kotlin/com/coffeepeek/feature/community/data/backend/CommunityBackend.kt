package com.coffeepeek.feature.community.data.backend

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.network.errorMessage
import com.coffeepeek.core.network.envelopeData
import com.coffeepeek.core.network.ApiEnvelope
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val CHECK_INS = "/api/v1/check-ins"

/** Plain endpoint calls. Every non-2xx answer becomes a typed failure; nothing here knows user-facing texts. */
internal class CommunityBackend(private val client: HttpClient) {
    suspend fun feed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPageDto> = requestResult {
        require(pageSize in 1..100) { "Page size must be 1..100" }
        listOfNotNull(filters.citySlug, filters.coffeeShopSlug, filters.authorSlug).forEach { require(it.length in 1..100) { "Invalid feed filter" } }
        val response = client.get("/api/v1/feed") {
            expectSuccess = false
            // The timeline and personalised votes are always read afresh.
            header(HttpHeaders.CacheControl, "no-store")
            parameter("pageSize", pageSize)
            cursor?.let { parameter("cursor", it) }
            filters.citySlug?.let { parameter("citySlug", it) }
            filters.coffeeShopSlug?.let { parameter("coffeeShopSlug", it) }
            filters.authorSlug?.let { parameter("authorSlug", it) }
        }
        if (!response.status.isSuccess()) {
            throw FeedLoadException("Feed request failed (${response.status.value})", restartPagination = response.status == HttpStatusCode.BadRequest && cursor != null)
        }
        response.envelope<FeedPageDto>()
    }

    suspend fun mine(page: Int, size: Int): Result<MyPostsDto> = requestResult {
        val response = client.get("$CHECK_INS/mine") {
            expectSuccess = false
            parameter("pageNumber", page.coerceAtLeast(1))
            parameter("pageSize", size.coerceIn(1, 100))
        }
        response.requireSuccess()
        response.envelope<MyPostsDto>()
    }

    suspend fun update(id: String, request: UpdateRequest): Result<PostDto> = requestResult {
        val response = client.put("$CHECK_INS/$id") { expectSuccess = false; contentType(ContentType.Application.Json); setBody(request) }
        response.requireSuccess()
        response.envelope<PostDto>().also { check(it.id.isNotBlank()) { "Missing check-in" } }
    }

    suspend fun setVisibility(id: String, visibility: String): Result<PostDto> = requestResult {
        val response = client.put("$CHECK_INS/$id/visibility") { expectSuccess = false; contentType(ContentType.Application.Json); setBody(VisibilityRequest(visibility)) }
        response.requireSuccess()
        response.envelope<PostDto>().also { check(it.id.isNotBlank()) { "Missing check-in" } }
    }

    suspend fun setHelpful(id: String, helpful: Boolean): Result<HelpfulDto> = requestResult {
        val path = "$CHECK_INS/$id/helpful"
        val response = if (helpful) client.put(path) { expectSuccess = false } else client.delete(path) { expectSuccess = false }
        response.requireSuccess()
        response.envelope<HelpfulDto>()
    }

    suspend fun report(id: String, text: String): Result<Unit> = requestResult {
        val response = client.post("$CHECK_INS/$id/reports") { expectSuccess = false; contentType(ContentType.Application.Json); setBody(ReportRequest(text)) }
        response.requireSuccess()
        val envelope = response.body<ApiEnvelope<kotlinx.serialization.json.JsonElement>>()
        if (response.status != HttpStatusCode.Created || !envelope.isSuccess || envelope.data == null) throw CommunityFailure(FailureKind.Other, envelope.message?.takeIf(String::isNotBlank))
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        throw CommunityFailure(
            when (status) {
                HttpStatusCode.Unauthorized -> FailureKind.SignInRequired
                HttpStatusCode.Forbidden -> FailureKind.Forbidden
                HttpStatusCode.NotFound -> FailureKind.NotFound
                else -> FailureKind.Other
            },
            errorMessage(),
        )
    }

    private suspend inline fun <reified T> HttpResponse.envelope(): T = envelopeData { CommunityFailure(FailureKind.Other, it) }
}
