package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.cookie
import io.ktor.client.request.put
import io.ktor.http.isSuccess

/** The exchange of a refresh token for a new credential pair; it carries no session of its own. */
fun interface TokenRefresh {
    suspend fun refresh(refreshToken: String): Result<SessionTokens>
}

/**
 * [plain] carries no credentials. A definitive rejection (400, 401, 403) fails with [RefreshRejectedException] so the session
 * is signed out; any other failure is temporary and leaves the session alone. The new refresh token is the one in the body,
 * else the `refreshToken` cookie, else the one that was sent.
 */
fun createTokenRefresh(plain: HttpClient): TokenRefresh = TokenRefresh { refreshToken ->
    requestResult {
        val token = refreshToken.takeIf { it.isNotBlank() } ?: throw AuthApiException("Refresh token отсутствует")
        val response = plain.put("/api/Tokens") {
            expectSuccess = false
            cookie("refreshToken", token)
        }
        if (!response.status.isSuccess()) {
            if (response.status.value in setOf(400, 401, 403)) throw RefreshRejectedException()
            throw AuthApiException("Refresh temporarily failed (${response.status.value})", response.status.value)
        }
        val envelope = response.body<Envelope<TokensDto>>()
        val data = envelope.data
        if (!envelope.isSuccess || data == null) throw AuthApiException(envelope.message)
        val resolved = data.refreshToken.takeIf { it.isNotBlank() } ?: response.refreshTokenCookie()?.takeIf { it.isNotBlank() } ?: token
        SessionTokens(data.accessToken, resolved)
    }
}
