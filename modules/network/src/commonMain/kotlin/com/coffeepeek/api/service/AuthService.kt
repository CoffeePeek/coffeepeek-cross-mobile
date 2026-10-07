package com.coffeepeek.api.service

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.request.GoogleLoginReq
import com.coffeepeek.api.model.request.LoginReq
import com.coffeepeek.api.model.request.RegistrationReq
import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.deleteResult
import com.coffeepeek.api.utils.extractRefreshToken
import com.coffeepeek.api.utils.postResult
import com.coffeepeek.api.utils.putResult
import com.coffeepeek.api.utils.setJsonBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class AuthService(
    private val client: HttpClient,
    private val refreshClient: HttpClient,
) {

    suspend fun login(email: String, password: String): Result<AuthResp> =
        runCatching {
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
        runCatching {
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

    suspend fun register(userName: String, email: String, password: String): Result<Unit> =
        runCatching {
            val response = refreshClient.postResult("/api/Users") {
                setJsonBody(RegistrationReq(email.trim().lowercase(), password, userName))
            }.getOrThrow()

            val apiResponse = response.body<ApiResponse<Unit>>()
            if (!apiResponse.isSuccess) {
                throw ApiException(apiResponse.message)
            }
        }

    suspend fun isEmailTaken(email: String): Result<Boolean> =
        runCatching {
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
        runCatching {
            val token = refreshToken?.takeIf { it.isNotBlank() }
                ?: throw ApiException("Refresh token отсутствует")

            val response = refreshClient.putResult("/api/Tokens") {
                cookie("refreshToken", token)
            }.getOrThrow()

            if (!response.status.isSuccess()) {
                throw ApiException("Refresh failed (${response.status.value})")
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
        runCatching {
            val response = client.deleteResult("/api/Tokens") {
                refreshToken?.takeIf { it.isNotBlank() }?.let { cookie("refreshToken", it) }
            }.getOrThrow()
            if (!response.status.isSuccess()) {
                throw ApiException("Logout failed")
            }
        }
}
