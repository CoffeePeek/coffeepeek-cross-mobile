package com.coffeepeek.data.repository

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.api.service.AuthService
import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.data.util.JwtUtils
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.domain.repository.SessionRepository

class AuthRepositoryImpl(
    private val authService: AuthService,
    private val sessionRepository: SessionRepository,
    private val userSessionCleaner: UserSessionCleaner,
) : AuthRepository {

    private fun AuthResp.toSession(): Session = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = JwtUtils.extractUserId(accessToken),
    )

    private suspend fun Result<Session>.persistSession(): Result<Session> =
        also { result -> result.onSuccess { sessionRepository.saveSession(it) } }

    override suspend fun login(email: String, password: String): Result<Session> =
        authService.login(email, password).map { it.toSession() }.persistSession()

    override suspend fun googleLogin(idToken: String): Result<Session> =
        authService.googleLogin(idToken).map { it.toSession() }.persistSession()

    override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> =
        authService.beginAppleLogin().map { AppleLoginChallenge(it.challengeId, it.nonce) }

    override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> =
        authService.appleLogin(challengeId, idToken).map { it.toSession() }.persistSession()

    override suspend fun register(userName: String, email: String, password: String): Result<Unit> =
        authService.register(userName, email, password)

    override suspend fun isEmailTaken(email: String): Result<Boolean> =
        authService.isEmailTaken(email)

    override suspend fun logout(): Result<Unit> {
        val refreshToken = sessionRepository.getSession()?.refreshToken
        userSessionCleaner.clearLocalUserData()
        runCatching { authService.logout(refreshToken) }
        return Result.success(Unit)
    }
}
