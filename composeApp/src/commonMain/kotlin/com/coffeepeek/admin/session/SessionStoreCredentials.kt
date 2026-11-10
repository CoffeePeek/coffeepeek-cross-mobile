package com.coffeepeek.admin.session

import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore

/**
 * The transport's view of the account's stored session. Only an active session yields credentials; a rotated pair is stored
 * with the user id read from its access token, and a refresh response without a refresh token keeps the stored one.
 */
internal class SessionStoreCredentials(private val sessions: SessionStore) : SessionCredentials {
    override suspend fun load(): SessionTokens? {
        sessions.warmCache()
        return sessions.peekSession()?.takeIf(sessions::isActiveSession)?.toTokens()
    }

    override suspend fun save(tokens: SessionTokens?) {
        val current = sessions.peekSession()
        sessions.saveSession(tokens?.let { it.toSession(current) })
    }

    override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
        val old = sessions.peekSession()
        if (old?.accessToken != expected.accessToken || old.refreshToken.orEmpty() != expected.refreshToken.orEmpty()) return false
        return sessions.replaceSession(old, tokens?.toSession(old))
    }
}

private fun Session.toTokens() = SessionTokens(accessToken, refreshToken)

private fun SessionTokens.toSession(current: Session?) = Session(
    accessToken = accessToken,
    refreshToken = refreshToken?.takeIf { it.isNotBlank() } ?: current?.refreshToken,
    userId = JwtUtils.extractUserId(accessToken) ?: current?.userId,
)
