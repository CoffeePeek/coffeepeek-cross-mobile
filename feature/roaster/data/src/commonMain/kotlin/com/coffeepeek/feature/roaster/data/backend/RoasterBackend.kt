package com.coffeepeek.feature.roaster.data.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess

internal class RoasterBackend(private val client: HttpClient) {
    suspend fun list(): List<RoasterSummaryDto> {
        val response = client.get("/api/roasters")
        return response.body<RoasterEnvelopeDto<List<RoasterSummaryDto>>>().require(response.status.isSuccess())
    }

    suspend fun detail(slug: String): RoasterDetailsDto {
        require(slug.isNotBlank() && slug.length <= 200)
        val response = client.get("/api/roasters") { url { appendPathSegments(slug, encodeSlash = true) } }
        return response.body<RoasterEnvelopeDto<RoasterDetailsDto>>().require(response.status.isSuccess())
    }
}

private fun <T> RoasterEnvelopeDto<T>.require(httpOk: Boolean): T {
    check(httpOk && isSuccess) { message ?: "Roaster request failed" }
    return checkNotNull(data) { message ?: "Roaster response has no data" }
}
