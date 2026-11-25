package com.coffeepeek.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Coordinates token rotation for the API and image clients of a session. */
class SessionRefreshCoordinator(private val session: BearerSession) {
    internal class Credentials(val tokens: SessionTokens, val observed: Attempt?)
    internal class Attempt(val previous: SessionTokens) {
        val result = CompletableDeferred<SessionTokens?>()
    }
    private class Exchange(val attempt: Attempt, val start: Boolean)

    private val mutex = Mutex()
    private var latest: Attempt? = null

    internal suspend fun credentials(): Credentials? {
        val snapshot = mutex.withLock { session.load()?.let { Credentials(it, latest) } } ?: return null
        val pending = snapshot.observed?.takeIf { !it.result.isCompleted && it.previous == snapshot.tokens }
            ?: return snapshot
        return awaitCurrent(pending)?.let { Credentials(it, pending) }
    }

    internal suspend fun refresh(rejected: Credentials): SessionTokens? {
        while (true) {
            val exchange = selectExchange(rejected) ?: return null
            if (exchange.start) complete(exchange.attempt)
            if (exchange.attempt.previous == rejected.tokens) return awaitCurrent(exchange.attempt)
            waitForOtherSession(exchange.attempt)
        }
    }

    private suspend fun selectExchange(rejected: Credentials): Exchange? = mutex.withLock {
        val previous = latest
        // A prior request shares the same outcome even if its 401 arrived just after the exchange completed.
        if (previous != null && previous.previous == rejected.tokens && previous !== rejected.observed) {
            return@withLock Exchange(previous, start = false)
        }
        if (session.load() != rejected.tokens) return@withLock null
        if (previous != null && !previous.result.isCompleted) return@withLock Exchange(previous, start = false)
        val attempt = Attempt(rejected.tokens).also { latest = it }
        Exchange(attempt, start = true)
    }

    private suspend fun awaitCurrent(attempt: Attempt): SessionTokens? {
        val updated = attempt.result.await()
        currentCoroutineContext().ensureActive()
        return updated?.takeIf { session.load() == it }
    }

    private suspend fun waitForOtherSession(attempt: Attempt) {
        try {
            attempt.result.await()
        } catch (_: Exception) {
            // An old account's outcome belongs to its own requests.
            currentCoroutineContext().ensureActive()
        }
    }

    private suspend fun complete(attempt: Attempt) = withContext(NonCancellable) {
        // The first request can go away after the server consumes its refresh token. Persist the response for its peers.
        try {
            attempt.result.complete(rotate(attempt.previous))
        } catch (failure: Throwable) {
            attempt.result.completeExceptionally(failure)
        }
    }

    private suspend fun rotate(previous: SessionTokens): SessionTokens? {
        val refreshToken = previous.refreshToken?.takeIf(String::isNotBlank)
        val updated = try {
            refreshToken?.let { session.refresh(it).getOrThrow() }
        } catch (_: RefreshRejectedException) {
            null
        }
        return updated.takeIf { session.replace(previous, updated) }
    }
}
