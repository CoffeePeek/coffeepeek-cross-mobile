package com.coffeepeek.admin.analytics

import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

internal suspend fun observeAnalyticsSession(service: AnalyticsService, sessions: SessionReader) {
    try {
        service.restoreConsent()
        sessions.getSession()
        sessions.observeSession().catch { service.syncIdentityNow(null, authRestoring = true) }.collect { session ->
            // Only the backend user id is an analytics identity; credentials never are.
            val active = sessions.isActiveSession(session)
            service.syncIdentity(session?.userId?.takeIf { active }, authRestoring = active && session?.userId == null)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        service.syncIdentityNow(null, authRestoring = true)
    }
}
