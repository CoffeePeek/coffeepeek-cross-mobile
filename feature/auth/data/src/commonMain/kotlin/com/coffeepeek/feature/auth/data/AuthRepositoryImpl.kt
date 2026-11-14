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
import io.ktor.client.request.cookie
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
 * [plain] is the client without credentials (sign-in, registration, e-mail check, recovery), [authenticated] the one that
 * carries the access token (sign-out). The session is written through [sessions]; local data of another account is
 * cleared through [localUserData] before it is replaced.
 */
internal class AuthRepositoryImpl(
    private val plain: HttpClient,
    private val authenticated: HttpClient,
    private val sessions: SessionStore,
    private val localUserData: LocalUserData,
) : AuthRepository {
    override suspend fun beginAppleSignIn(): Result<AppleSignInChallenge> = requestResult {
        AppleAuthBackend(plain).challenge()
    }

    override suspend fun signInWithApple(challengeId: String, idToken: String): Result<SignedInAccount> = requestResult {
        persist(AppleAuthBackend(plain).signIn(challengeId, idToken))
    }

    override suspend fun signIn(email: String, password: String): Result<SignedInAccount> = requestResult {
        val response = plain.post("/api/Tokens") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email.trim().lowercase(), password))
        }
        persist(response.tokens())
    }

    override suspend fun signInWithGoogle(idToken: String): Result<SignedInAccount> = requestResult {
        val response = plain.post("/api/Tokens/google/login") {
            contentType(ContentType.Application.Json)
            setBody(GoogleLoginRequest(idToken))
        }
        persist(response.tokens())
    }

    override suspend fun register(userName: String, email: String, password: String): Result<Unit> = requestResult {
        val response = plain.post("/api/Users") {
            contentType(ContentType.Application.Json)
            setBody(RegistrationRequest(email.trim().lowercase(), password, userName))
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
        val response = authenticated.delete("/api/Tokens") {
            refreshToken?.takeIf { it.isNotBlank() }?.let { cookie("refreshToken", it) }
        }
        if (!response.status.isSuccess()) throw AuthApiException("Logout failed")
    }

    private suspend fun HttpResponse.tokens(): TokensDto {
        val envelope = body<Envelope<TokensDto>>()
        val data = envelope.data
        if (!envelope.isSuccess || data == null) throw AuthApiException(envelope.message)
        return data.copy(refreshToken = refreshTokenCookie()?.takeIf { it.isNotBlank() } ?: data.refreshToken)
    }

    private suspend fun persist(tokens: TokensDto): SignedInAccount {
        val next = Session(tokens.accessToken, tokens.refreshToken, JwtUtils.extractUserId(tokens.accessToken))
        if (sessions.getSession()?.userId != next.userId) localUserData.clear()
        sessions.saveSession(next)
        return SignedInAccount(next.userId)
    }
}
