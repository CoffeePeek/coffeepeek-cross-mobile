package com.coffeepeek.core.network

import kotlin.time.Instant

/** Transport credentials only; feature DTOs and storage implementations stay outside core. */
class SessionTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: Instant? = null,
    val refreshTokenExpiresAt: Instant? = null,
) {
    override fun equals(other: Any?): Boolean = this === other ||
        other is SessionTokens && accessToken == other.accessToken && refreshToken == other.refreshToken &&
        accessTokenExpiresAt == other.accessTokenExpiresAt && refreshTokenExpiresAt == other.refreshTokenExpiresAt
    override fun hashCode(): Int {
        var hash = accessToken.hashCode()
        hash = 31 * hash + refreshToken.hashCode()
        hash = 31 * hash + accessTokenExpiresAt.hashCode()
        return 31 * hash + refreshTokenExpiresAt.hashCode()
    }
    override fun toString(): String = "SessionTokens(redacted)"
}

/** The account's stored credentials; the callbacks must not use an authenticated client. */
interface SessionCredentials {
    suspend fun load(): SessionTokens?
    suspend fun save(tokens: SessionTokens?)
    /** Atomically compare and replace the current credential snapshot. */
    suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean
}

/** Credentials plus the exchange of a refresh token, which uses a separate plain client. */
interface BearerSession : SessionCredentials {
    suspend fun refresh(refreshToken: String): Result<SessionTokens>
}
