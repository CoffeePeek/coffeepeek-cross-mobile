package com.coffeepeek.feature.auth.data

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.time.Instant

@Serializable
internal data class NativeRefreshRequest(val refreshToken: String) {
    override fun toString(): String = "NativeRefreshRequest([redacted])"
}

@Serializable
internal data class NativeTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String,
) {
    override fun toString(): String = "NativeTokensDto([redacted])"
}

internal suspend fun HttpResponse.nativeTokens(): TokensDto {
    if (status.value == 429 || status.value >= 500) throw AuthApiException("Token request temporarily failed", status.value)
    val envelope = nativeEnvelope()
    val data = envelope.data
    if (!status.isSuccess() || !envelope.isSuccess || data == null) throw AuthApiException(envelope.message, status.value)
    if (data.accessToken.isBlank() || data.refreshToken.isBlank()) throw AuthApiException("Invalid native token response")
    return TokensDto(data.accessToken, data.refreshToken,
        nativeExpiry(data.accessTokenExpiresAt), nativeExpiry(data.refreshTokenExpiresAt))
}

private suspend fun HttpResponse.nativeEnvelope(): Envelope<NativeTokensDto> = try {
    body<Envelope<NativeTokensDto>>()
} catch (_: ContentConvertException) {
    throw AuthApiException("Invalid native token response", status.value)
} catch (_: NoTransformationFoundException) {
    throw AuthApiException("Invalid native token response", status.value)
} catch (_: SerializationException) {
    throw AuthApiException("Invalid native token response", status.value)
}

private fun nativeExpiry(value: String): Instant = try {
    Instant.parse(value)
} catch (_: IllegalArgumentException) {
    throw AuthApiException("Invalid native token expiry")
}
