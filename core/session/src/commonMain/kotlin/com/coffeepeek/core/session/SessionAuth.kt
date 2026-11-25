package com.coffeepeek.core.session

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object SessionAuth {
    fun isActive(session: Session?): Boolean = isActive(session, Clock.System.now())

    internal fun isActive(session: Session?, now: Instant): Boolean {
        if (session == null) return false
        if (session.accessToken.isBlank() || session.userId.isNullOrBlank()) return false
        // A local clock cannot reject refresh credentials; only the server can invalidate them.
        if (!session.refreshToken.isNullOrBlank()) return true
        val expired = session.accessTokenExpiresAt?.let { now >= it - 30.seconds }
            ?: JwtUtils.extractExp(session.accessToken)?.let { now.epochSeconds >= it - 30 }
            ?: false
        return !expired
    }
}
