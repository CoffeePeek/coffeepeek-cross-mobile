package com.coffeepeek.core.network

/** Transport credentials only; feature DTOs and storage implementations stay outside core. */
class SessionTokens(val accessToken: String, val refreshToken: String?) {
    override fun equals(other: Any?): Boolean = this === other ||
        other is SessionTokens && accessToken == other.accessToken && refreshToken == other.refreshToken
    override fun hashCode(): Int = 31 * accessToken.hashCode() + (refreshToken?.hashCode() ?: 0)
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
