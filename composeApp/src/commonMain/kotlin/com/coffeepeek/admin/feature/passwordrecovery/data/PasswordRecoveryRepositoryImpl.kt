package com.coffeepeek.admin.feature.passwordrecovery.data

import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class PasswordRecoveryRepositoryImpl(private val client: HttpClient) : PasswordRecoveryRepository {
    override suspend fun requestReset(email: String): Result<Unit> = requestResult {
        val response = client.post("/api/Users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email.trim().lowercase()))
        }
        check(response.status.isSuccess()) { "Password reset request failed" }
        check(response.body<RecoveryResponse>().isSuccess) { "Password reset request rejected" }
    }

    override suspend fun resetPassword(tokenOrLink: String, newPassword: String): Result<Unit> = requestResult {
        val input = tokenOrLink.trim()
        val token = if (input.contains("://")) {
            val link = Url(input)
            require(link.protocol.name == "https" && link.host.equals("coffeepeek.by", ignoreCase = true) &&
                link.encodedPath.trimEnd('/') == "/reset-password") { "Invalid reset link" }
            link.parameters["token"].orEmpty()
        } else input
        require(token.isNotBlank()) { "Missing reset token" }
        val response = client.post("/api/Users/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(token, newPassword))
        }
        check(response.status.isSuccess()) { "Password reset failed" }
        check(response.body<RecoveryResponse>().isSuccess) { "Password reset rejected" }
    }
}

@Serializable
private data class ForgotPasswordRequest(val email: String)

@Serializable
private data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
private data class RecoveryResponse(val isSuccess: Boolean)
