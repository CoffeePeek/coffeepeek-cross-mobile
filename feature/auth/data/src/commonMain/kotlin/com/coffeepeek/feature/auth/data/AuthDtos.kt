@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** The server's envelope; `data` is read as the type each call expects. */
@Serializable
internal data class Envelope<T>(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean = false,
    @SerialName("message") @JsonNames("Message") val message: String? = null,
    @SerialName("data") @JsonNames("Data") val data: T? = null,
)

@Serializable
internal data class TokensDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String = "",
)

@Serializable
internal data class LoginRequest(@SerialName("email") val email: String, @SerialName("password") val password: String)

@Serializable
internal data class GoogleLoginRequest(@SerialName("idToken") val idToken: String)

@Serializable
internal data class RegistrationRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("userName") val userName: String,
)

@Serializable
internal data class ForgotPasswordRequest(@SerialName("email") val email: String)

/** The server answered, but not with something that signs the user in or registers them. */
internal class AuthApiException(message: String?, val statusCode: Int? = null) : Exception() {
    override val message: String = message?.takeIf { it.isNotBlank() } ?: "Сервер вернул некорректный ответ"
}
