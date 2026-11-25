package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
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
 * [plain] carries no credentials. Only HTTP 401 definitively rejects the refresh session.
 * Every successful response must contain the rotated token pair and both server expiry timestamps.
 */
fun createTokenRefresh(plain: HttpClient): TokenRefresh = TokenRefresh { refreshToken ->
    requestResult {
        val token = refreshToken.takeIf { it.isNotBlank() } ?: throw AuthApiException("Refresh token отсутствует")
        val response = plain.put("/api/tokens/native") {
            expectSuccess = false
            timeout { requestTimeoutMillis = 15_000 }
            contentType(ContentType.Application.Json)
            setBody(NativeRefreshRequest(token))
        }
        if (!response.status.isSuccess()) {
            if (response.status.value == 401) throw RefreshRejectedException()
            throw AuthApiException("Refresh temporarily failed (${response.status.value})", response.status.value)
        }
        val data = response.nativeTokens()
        if (data.refreshToken == token) throw AuthApiException("Refresh token did not rotate")
        SessionTokens(data.accessToken, data.refreshToken, data.accessTokenExpiresAt, data.refreshTokenExpiresAt)
    }
}
