package com.coffeepeek.core.session

import kotlin.time.Instant

data class Session(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
    val accessTokenExpiresAt: Instant? = null,
    val refreshTokenExpiresAt: Instant? = null,
) {
    override fun toString(): String = "Session(<redacted>)"
}
