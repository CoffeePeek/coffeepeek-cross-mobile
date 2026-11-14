package com.coffeepeek.feature.settings.data.backend

import com.coffeepeek.core.network.envelopeData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class AnalyticsPreferenceBackend(private val client: HttpClient) {
    suspend fun load(accountId: String): Boolean = client.get(PATH) {
        expectSuccess = false
        header(HttpHeaders.CacheControl, "no-store")
    }.preference(accountId)

    suspend fun set(accountId: String, enabled: Boolean, onlyIfUnset: Boolean): Boolean = client.put(PATH) {
        expectSuccess = false
        contentType(ContentType.Application.Json)
        setBody(AnalyticsPreferenceRequest(accountId, enabled, onlyIfUnset))
    }.preference(accountId)

    private suspend fun HttpResponse.preference(accountId: String): Boolean {
        check(status.isSuccess()) { "Analytics preference request failed (${status.value})" }
        val result: AnalyticsPreferenceDto = envelopeData { IllegalStateException(it) }
        check(result.accountId == accountId) { "Analytics preference account changed" }
        return result.enabled
    }

    private companion object { const val PATH = "/api/Users/me/analytics-preference" }
}

@Serializable
private data class AnalyticsPreferenceDto(val accountId: String, val enabled: Boolean)

@Serializable
private data class AnalyticsPreferenceRequest(val accountId: String, val enabled: Boolean, val onlyIfUnset: Boolean)
