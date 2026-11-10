package com.coffeepeek.admin.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Retry transport failures only while this connection still belongs to the session. */
internal suspend fun reconnectSession(
    isCurrent: () -> Boolean,
    connect: suspend () -> Unit,
    pause: suspend () -> Unit,
) {
    while (currentCoroutineContext().isActive && isCurrent()) {
        try {
            connect()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // An ordinary transport failure may reconnect after the configured delay.
        }
        if (isCurrent()) pause()
    }
}
