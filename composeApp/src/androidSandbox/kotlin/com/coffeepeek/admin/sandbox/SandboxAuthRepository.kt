package com.coffeepeek.admin.sandbox

import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.repository.AuthRepository

/** Credentials are neither stored nor transmitted; email/password exercises only local navigation. */
internal class SandboxAuthRepository(private val sessions: SandboxSessionRepository) : AuthRepository {
    override suspend fun login(email: String, password: String) = Result.success(run {
        sessions.login(); requireNotNull(sessions.peekSession())
    })
    override suspend fun googleLogin(idToken: String) = Result.failure<com.coffeepeek.domain.model.Session>(UnsupportedOperationException("OAuth is disabled in offline Sandbox"))
    override suspend fun beginAppleLogin() = Result.failure<AppleLoginChallenge>(UnsupportedOperationException("OAuth is disabled in offline Sandbox"))
    override suspend fun appleLogin(challengeId: String, idToken: String) = googleLogin(idToken)
    override suspend fun register(userName: String, email: String, password: String) = Result.success(Unit)
    override suspend fun isEmailTaken(email: String) = Result.success(false)
    override suspend fun logout() = Result.success(sessions.applySession(null))
}
