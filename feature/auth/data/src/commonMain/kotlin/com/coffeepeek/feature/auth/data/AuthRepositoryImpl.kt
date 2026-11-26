package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.auth.api.LocalUserData
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.AppleSignInChallenge
import com.coffeepeek.feature.auth.domain.SignedInAccount
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement

/**
 * [plain] carries no credentials; native logout identifies its session by the JSON refresh token.
 * The session is written through [sessions]; another account is cleared through [localUserData] before replacement.
 */
internal class AuthRepositoryImpl(
    private val plain: HttpClient,
    private val sessions: SessionStore,
    private val localUserData: LocalUserData,
) : AuthRepository {
    override suspend fun beginAppleSignIn(): Result<AppleSignInChallenge> = requestResult {
        AppleAuthBackend(plain).challenge()
    }

    override suspend fun signInWithApple(challengeId: String, idToken: String, analyticsEnabled: Boolean?): Result<SignedInAccount> = requestResult {
        persist(AppleAuthBackend(plain).signIn(challengeId, idToken, analyticsEnabled))
    }

    override suspend fun signIn(email: String, password: String): Result<SignedInAccount> = requestResult {
        val response = plain.post("/api/tokens/native") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email.trim().lowercase(), password))
        }
        persist(response.nativeTokens())
    }

    override suspend fun signInWithGoogle(idToken: String, analyticsEnabled: Boolean?): Result<SignedInAccount> = requestResult {
        val response = plain.post("/api/tokens/google/login") {
            contentType(ContentType.Application.Json)
            setBody(GoogleLoginRequest(idToken, analyticsEnabled))
        }
        persist(response.googleTokens())
    }

    override suspend fun register(userName: String, email: String, password: String, analyticsEnabled: Boolean): Result<Unit> = requestResult {
        val response = plain.post("/api/Users") {
            contentType(ContentType.Application.Json)
            setBody(RegistrationRequest(email.trim().lowercase(), password, userName, analyticsEnabled))
        }
        val envelope = response.body<Envelope<JsonElement>>()
        if (!envelope.isSuccess) throw AuthApiException(envelope.message)
    }

    override suspend fun isEmailTaken(email: String): Result<Boolean> = requestResult {
        val response = plain.get("/api/Users/exists") { parameter("email", email.trim().lowercase()) }
        if (response.status == HttpStatusCode.NotFound) {
            false
        } else {
            val envelope = response.body<Envelope<Boolean>>()
            envelope.isSuccess && envelope.data == true
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = requestResult {
        val response = plain.post("/api/Users/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email.trim().lowercase()))
        }
        check(response.status.isSuccess()) { "Password reset request failed" }
        check(response.body<Envelope<JsonElement>>().isSuccess) { "Password reset request rejected" }
    }

    override suspend fun signOut(): Result<Unit> {
        val refreshToken = sessions.getSession()?.refreshToken
        val cleanupFailure = try {
            localUserData.clear()
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            error
        }
        try {
            remoteSignOut(refreshToken)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The server call is best effort; the local cleanup above is authoritative.
        }
        return cleanupFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    private suspend fun remoteSignOut(refreshToken: String?) {
        val token = refreshToken?.takeIf(String::isNotBlank) ?: return
        val response = plain.delete("/api/tokens/native") {
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(NativeRefreshRequest(token))
        }
        if (!response.status.isSuccess()) throw AuthApiException("Logout failed")
    }

    private suspend fun HttpResponse.googleTokens(): TokensDto {
        val envelope = body<Envelope<TokensDto>>()
        val data = envelope.data
        if (!status.isSuccess() || !envelope.isSuccess || data == null) throw AuthApiException(envelope.message)
        val refresh = googleRefreshTokenCookie()?.takeIf(String::isNotBlank) ?: data.refreshToken
        if (data.accessToken.isBlank() || refresh.isBlank()) throw AuthApiException("Missing Google session")
        return data.copy(refreshToken = refresh)
    }

    private suspend fun persist(tokens: TokensDto): SignedInAccount {
        val next = Session(tokens.accessToken, tokens.refreshToken, JwtUtils.extractUserId(tokens.accessToken),
            accessTokenExpiresAt = tokens.accessTokenExpiresAt, refreshTokenExpiresAt = tokens.refreshTokenExpiresAt)
        if (sessions.getSession()?.userId != next.userId) localUserData.clear()
        sessions.saveSession(next)
        return SignedInAccount(next.userId)
    }
}
