package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.GoogleLoginReq
import com.coffeepeek.api.model.request.LoginReq
import com.coffeepeek.api.model.request.RegistrationReq
import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.api.model.response.AppleLoginChallengeResp
import com.coffeepeek.api.model.request.AppleLoginReq
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.ServiceUnavailableException
import com.coffeepeek.api.utils.deleteResult
import com.coffeepeek.api.utils.extractRefreshToken
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.putResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

class AuthService(
    private val client: HttpClient,
    private val refreshClient: HttpClient,
) {

    suspend fun login(email: String, password: String): Result<AuthResp> =
        requestResult {
            val response = refreshClient.postResult("/api/Tokens") {
                setJsonBody(LoginReq(email.trim().lowercase(), password))
            }.getOrThrow()

            val apiResponse = response.body<ApiResponse<AuthResp>>()
            if (!apiResponse.isSuccess || apiResponse.data == null) {
                throw ApiException(apiResponse.message)
            }

            apiResponse.data.copy(
                refreshToken = response.extractRefreshToken()
                    ?.takeIf { it.isNotBlank() }
                    ?: apiResponse.data.refreshToken,
            )
        }

    suspend fun googleLogin(idToken: String): Result<AuthResp> =
        requestResult {
            val response = refreshClient.postResult("/api/Tokens/google/login") {
                setJsonBody(GoogleLoginReq(idToken))
            }.getOrThrow()

            val apiResponse = response.body<ApiResponse<AuthResp>>()
            if (!apiResponse.isSuccess || apiResponse.data == null) {
                throw ApiException(apiResponse.message)
            }

            apiResponse.data.copy(
                refreshToken = response.extractRefreshToken()
                    ?.takeIf { it.isNotBlank() }
                    ?: apiResponse.data.refreshToken,
            )
        }

    suspend fun beginAppleLogin(): Result<AppleLoginChallengeResp> = requestResult {
        val response = refreshClient.postResult("/api/Tokens/apple/challenge").getOrThrow()
            .body<ApiResponse<AppleLoginChallengeResp>>()
        if (!response.isSuccess || response.data == null) throw ApiException(response.message)
        response.data
    }

    suspend fun appleLogin(challengeId: String, idToken: String): Result<AuthResp> = requestResult {
        val response = try {
            refreshClient.postResult("/api/Tokens/apple/login") {
                expectSuccess = false
                setJsonBody(AppleLoginReq(challengeId, idToken))
            }.getOrThrow()
        } catch (error: ServiceUnavailableException) {
            // The shared 5xx validator can reject the request before a response is returned here.
            throw ApiException("${error.message} [ASI-H${error.statusCode}]", error.statusCode)
        } catch (error: ApiException) {
            val status = error.statusCode?.takeIf { it in 400..599 } ?: throw error
            throw ApiException("${error.message} [ASI-H$status]", status)
        }
        val apiResponse = response.readAppleLoginResponseOrNull()
        if (response.status.value in 400..599) {
            val message = apiResponse?.message?.takeIf(String::isNotBlank)
                ?: "Сервер отклонил вход через Apple. Попробуйте ещё раз."
            throw ApiException("$message [ASI-H${response.status.value}]", response.status.value)
        }
        if (!response.status.isSuccess() || apiResponse == null) {
            throw ApiException("Сервер вернул некорректный ответ при входе через Apple. [ASI-R]")
        }
        if (!apiResponse.isSuccess) {
            val message = apiResponse.message?.takeIf(String::isNotBlank)
                ?: "Сервер отклонил вход через Apple. Попробуйте ещё раз."
            throw ApiException("$message [ASI-A]")
        }
        val session = apiResponse.data
        val accessToken = session?.accessToken?.takeIf(String::isNotBlank)
        val refreshToken = session?.refreshToken?.takeIf(String::isNotBlank)
        if (accessToken == null || refreshToken == null) {
            throw ApiException("Сервер не вернул сессию Apple. Попробуйте ещё раз. [ASI-I]")
        }
        AuthResp(accessToken, refreshToken)
    }

    suspend fun register(userName: String, email: String, password: String): Result<Unit> =
        requestResult {
            val response = refreshClient.postResult("/api/Users") {
                setJsonBody(RegistrationReq(email.trim().lowercase(), password, userName))
            }.getOrThrow()

            val apiResponse = response.body<ApiResponse<Unit>>()
            if (!apiResponse.isSuccess) {
                throw ApiException(apiResponse.message)
            }
        }

    suspend fun isEmailTaken(email: String): Result<Boolean> =
        requestResult {
            val response = refreshClient.get("/api/Users/exists") {
                parameter("email", email.trim().lowercase())
            }
            when (response.status) {
                HttpStatusCode.NotFound -> false
                else -> {
                    val apiResponse = response.body<ApiResponse<Boolean>>()
                    apiResponse.isSuccess && apiResponse.data == true
                }
            }
        }

    suspend fun refresh(refreshToken: String?) =
        requestResult {
            val token = refreshToken?.takeIf { it.isNotBlank() }
                ?: throw ApiException("Refresh token отсутствует")

            val response = refreshClient.putResult("/api/Tokens") {
                cookie("refreshToken", token)
            }.getOrThrow()

            if (!response.status.isSuccess()) {
                if (response.status.value in setOf(400, 401, 403)) throw RefreshRejectedException()
                throw ApiException("Refresh temporarily failed (${response.status.value})")
            }

            val apiResponse = response.body<ApiResponse<AuthResp>>()
            if (!apiResponse.isSuccess || apiResponse.data == null) {
                throw ApiException(apiResponse.message)
            }

            val bodyToken = apiResponse.data.refreshToken
            val cookieToken = response.extractRefreshToken()
            val resolvedRefresh = when {
                !bodyToken.isNullOrBlank() -> bodyToken
                !cookieToken.isNullOrBlank() -> cookieToken
                else -> token
            }

            apiResponse.data.copy(refreshToken = resolvedRefresh)
        }

    suspend fun logout(refreshToken: String?) =
        requestResult {
            val response = client.deleteResult("/api/Tokens") {
                refreshToken?.takeIf { it.isNotBlank() }?.let { cookie("refreshToken", it) }
            }.getOrThrow()
            if (!response.status.isSuccess()) {
                throw ApiException("Logout failed")
            }
        }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
private data class AppleLoginResponse(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean,
    @SerialName("message") @JsonNames("Message") val message: String? = null,
    @SerialName("data") @JsonNames("Data") val data: AppleLoginSession? = null,
)

@Serializable
private data class AppleLoginSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

private suspend fun HttpResponse.readAppleLoginResponseOrNull(): AppleLoginResponse? = try {
    body<AppleLoginResponse?>()
} catch (_: ContentConvertException) {
    null
} catch (_: NoTransformationFoundException) {
    null
}
