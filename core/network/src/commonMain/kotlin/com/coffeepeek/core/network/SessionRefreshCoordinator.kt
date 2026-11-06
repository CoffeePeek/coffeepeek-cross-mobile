package com.coffeepeek.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Every client of one session (API, images) shares one refresh-token rotation, so concurrent rejections
 * rotate the credential once. A rotation never survives an account change and is never reused for another account.
 */
class SessionRefreshCoordinator(private val session: BearerSession) {
    private val mutex = Mutex()
    private var lastRotation: Pair<SessionTokens, SessionTokens>? = null

    suspend fun refresh(refreshToken: String, rejectedAccessToken: String): SessionTokens = mutex.withLock {
        val before = session.load() ?: throw CancellationException("Session changed")
        if (before.accessToken != rejectedAccessToken) {
            // Reuse only a rotation performed here for this exact previous credential.
            // A login/account switch must never retry another account's operation.
            return@withLock lastRotation?.takeIf { it.first.accessToken == rejectedAccessToken && it.second == before }?.second
                ?: throw CancellationException("Session changed")
        }
        if (before.refreshToken != refreshToken) throw CancellationException("Session changed")
        val updated = session.refresh(refreshToken).getOrThrow()
        if (session.load() != before) throw CancellationException("Session changed")
        if (!session.replace(before, updated)) throw CancellationException("Session changed")
        lastRotation = before to updated
        updated
    }
}
