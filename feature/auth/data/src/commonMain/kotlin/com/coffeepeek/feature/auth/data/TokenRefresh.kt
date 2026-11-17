package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** The exchange of a refresh token for a new credential pair; it carries no session of its own. */
fun interface TokenRefresh {
    suspend fun refresh(refreshToken: String): Result<SessionTokens>
}

/**
 * [plain] sends the refresh token as JSON without bearer authentication. A definitive rejection
 * (400, 401, 403) fails with [RefreshRejectedException]. A successful exchange requires a complete
 * token pair with a rotated refresh token.
 */
fun createTokenRefresh(plain: HttpClient): TokenRefresh = TokenRefresh { refreshToken ->
    requestResult {
        val token = refreshToken.takeIf { it.isNotBlank() } ?: throw AuthApiException("Refresh token отсутствует")
        val response = plain.put("/api/Tokens/native") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(NativeRefreshRequest(token))
        }
        if (!response.status.isSuccess()) {
            if (response.status.value in setOf(400, 401, 403)) throw RefreshRejectedException()
            throw AuthApiException("Refresh temporarily failed (${response.status.value})", response.status.value)
        }
        val envelope = response.body<Envelope<TokensDto>>()
        val data = envelope.data?.takeIf { envelope.isSuccess } ?: throw AuthApiException(envelope.message)
        if (data.accessToken.isBlank() || data.refreshToken.isBlank() || data.refreshToken == token) {
            throw AuthApiException(null)
        }
        SessionTokens(data.accessToken, data.refreshToken)
    }
}
