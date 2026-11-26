package com.coffeepeek.core.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Persisted shape of a session. The field names are the stored JSON and must not change. */
@Serializable
data class SessionCredentials(
    val accessToken: String,
    val refreshToken: String = "",
    val accessTokenExpiresAt: Instant? = null,
    val refreshTokenExpiresAt: Instant? = null,
) {
    override fun toString(): String = "SessionCredentials(<redacted>)"
}

internal val SessionJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
