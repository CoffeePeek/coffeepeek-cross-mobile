package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** API and uncached images share one refresh-token rotation. No cross-account retry. */
internal class SessionRefreshCoordinator(
    private val current: () -> AuthResp?,
    private val save: (AuthResp?) -> Unit,
    private val refresh: suspend (String) -> AuthResp,
) {
    private val mutex = Mutex()
    private var lastRotation: Pair<AuthResp, AuthResp>? = null

    suspend fun refresh(refreshToken: String, rejectedAccessToken: String): AuthResp = mutex.withLock {
        val before = current() ?: throw CancellationException("Session changed")
        if (before.accessToken != rejectedAccessToken) {
            // Reuse only a rotation performed here for this exact previous credential.
            // A login/account switch must never retry another account's operation.
            return@withLock lastRotation?.takeIf { it.first.accessToken == rejectedAccessToken && it.second == before }?.second
                ?: throw CancellationException("Session changed")
        }
        if (before.refreshToken != refreshToken) throw CancellationException("Session changed")
        val updated = refresh.invoke(refreshToken)
        if (current() != before) throw CancellationException("Session changed")
        save(updated)
        lastRotation = before to updated
        updated
    }
}
