package com.coffeepeek.admin.feature.passwordrecovery.data

import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
}

@Serializable
private data class ForgotPasswordRequest(val email: String)

@Serializable
private data class RecoveryResponse(val isSuccess: Boolean)
