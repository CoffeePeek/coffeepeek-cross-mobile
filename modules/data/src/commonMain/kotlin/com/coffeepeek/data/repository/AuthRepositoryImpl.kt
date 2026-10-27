package com.coffeepeek.data.repository

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.api.service.AuthService
import com.coffeepeek.data.session.LocalUserDataCleanup
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException

class AuthRepositoryImpl(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
    private val localUserData: LocalUserDataCleanup,
) : AuthRepository {

    private fun AuthResp.toSession(): Session = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = JwtUtils.extractUserId(accessToken),
    )

    private suspend fun Result<Session>.persistSession(): Result<Session> =
        also { result -> result.onSuccess { next ->
            if (sessionRepository.getSession()?.userId != next.userId) {
                localUserData.clear()
            }
            sessionRepository.saveSession(next)
        } }

    override suspend fun login(email: String, password: String): Result<Session> =
        authService.login(email, password).map { it.toSession() }.persistSession()

    override suspend fun googleLogin(idToken: String): Result<Session> =
        authService.googleLogin(idToken).map { it.toSession() }.persistSession()

    override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> =
        authService.beginAppleLogin().map { AppleLoginChallenge(it.challengeId, it.nonce) }

    override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> {
        val result = authService.appleLogin(challengeId, idToken).map { it.toSession() }
        val session = result.getOrElse { error ->
            if (error is CancellationException) throw error
            return result
        }
        return try {
            sessionRepository.saveSession(session)
            result
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(IllegalStateException("Не удалось сохранить сессию Apple. Попробуйте ещё раз. [ASI-P]", error))
        }
    }

    override suspend fun register(userName: String, email: String, password: String): Result<Unit> =
        authService.register(userName, email, password)

    override suspend fun isEmailTaken(email: String): Result<Boolean> =
        authService.isEmailTaken(email)

    override suspend fun logout(): Result<Unit> {
        val refreshToken = sessionRepository.getSession()?.refreshToken
        val cleanupFailure = try {
            localUserData.clear()
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            error
        }
        try {
            authService.logout(refreshToken).onFailure { error ->
                if (error is CancellationException) throw error
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Remote logout is best-effort; local cleanup is authoritative.
        }
        return cleanupFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
