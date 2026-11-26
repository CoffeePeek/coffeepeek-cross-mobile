package com.coffeepeek.admin.session

import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore

/**
 * The transport's view of the account's stored session. Rotation replaces the whole snapshot atomically.
 */
internal class SessionStoreCredentials(
    private val sessions: SessionStore,
    private val onSessionRejected: () -> Unit = {},
) : SessionCredentials {
    override suspend fun load(): SessionTokens? {
        sessions.warmCache()
        return sessions.peekSession()?.takeIf(sessions::isActiveSession)?.toTokens()
    }

    override suspend fun save(tokens: SessionTokens?) {
        val current = sessions.peekSession()
        sessions.saveSession(tokens?.let { it.toSession(current) })
    }

    override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
        val old = sessions.peekSession() ?: return false
        if (old.toTokens() != expected) return false
        val next = tokens?.toSession(old)
        if (next != null && next.userId != old.userId) return false
        val replaced = sessions.replaceSession(old, next)
        if (replaced && next == null && sessions.peekSession() == null) onSessionRejected()
        return replaced
    }
}

private fun Session.toTokens() = SessionTokens(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt)

private fun SessionTokens.toSession(current: Session?) = Session(
    accessToken = accessToken,
    refreshToken = refreshToken?.takeIf(String::isNotBlank),
    userId = JwtUtils.extractUserId(accessToken) ?: current?.userId,
    accessTokenExpiresAt = accessTokenExpiresAt,
    refreshTokenExpiresAt = refreshTokenExpiresAt,
)
