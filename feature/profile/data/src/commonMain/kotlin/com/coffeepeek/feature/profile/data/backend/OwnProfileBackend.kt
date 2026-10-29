package com.coffeepeek.feature.profile.data.backend

import com.coffeepeek.core.network.ApiEnvelope
import com.coffeepeek.core.network.errorMessage
import com.coffeepeek.core.network.envelopeData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

internal class OwnProfileBackend(private val client: HttpClient) {
    suspend fun load(): OwnProfileDto = client.get("/api/Users/me") {
        expectSuccess = false
        header(HttpHeaders.CacheControl, "no-store")
    }.requireSuccess().envelopeData { IllegalStateException(it) }

    suspend fun username(value: String): UsernameResponse {
        val response = client.patch("/api/Users/me/username") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(UsernameRequest(value))
        }
        if (!response.status.isSuccess()) throw usernameFailure(response.status.value, response.errorMessage())
        return response.envelopeData { usernameFailure(response.status.value, it) }
    }

    suspend fun about(value: String) {
        client.patch("/api/Users/me/about") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(AboutRequest(value))
        }.requireSuccess()
    }

    suspend fun avatar(photo: UploadedAvatar) {
        client.patch("/api/Users/me/avatar") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(AvatarRequest(photo))
        }.requireSuccess()
    }

    suspend fun requestDeletion(): DeletionResponse = client.delete("/api/Users/me") {
        expectSuccess = false
    }.requireSuccess().envelopeData { IllegalStateException(it) }

    suspend fun existingDeletion(): DeletionResponse? {
        val response = client.get("/api/Users/me/deletion-request") { expectSuccess = false }
        if (response.status == HttpStatusCode.NotFound) return null
        response.requireSuccess()
        val result = response.body<ApiEnvelope<DeletionResponse>>()
        if (result.isSuccess && result.data != null) return result.data
        if (result.message.orEmpty().contains("not found", ignoreCase = true)) return null
        error(result.message ?: "Не удалось загрузить запрос удаления")
    }
}

private suspend fun HttpResponse.requireSuccess(): HttpResponse {
    check(status.isSuccess()) { errorMessage() ?: "Ошибка профиля (${status.value})" }
    return this
}
