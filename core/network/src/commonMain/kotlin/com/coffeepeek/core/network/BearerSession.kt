package com.coffeepeek.core.network

/** Transport credentials only; feature DTOs and storage implementations stay outside core. */
class SessionTokens(val accessToken: String, val refreshToken: String?) {
    override fun equals(other: Any?): Boolean = this === other ||
        other is SessionTokens && accessToken == other.accessToken && refreshToken == other.refreshToken
    override fun hashCode(): Int = 31 * accessToken.hashCode() + (refreshToken?.hashCode() ?: 0)
    override fun toString(): String = "SessionTokens(redacted)"
}

/** Callbacks must not use the authenticated client; refresh uses a separate plain client. */
interface BearerSession {
    suspend fun load(): SessionTokens?
    suspend fun save(tokens: SessionTokens?)
    /** Atomically compare and replace the current credential snapshot. */
    suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean
    suspend fun refresh(refreshToken: String): Result<SessionTokens>
}
