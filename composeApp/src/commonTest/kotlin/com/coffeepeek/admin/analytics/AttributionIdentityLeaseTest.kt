package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.di.attribution.permittedIdentity
import com.coffeepeek.core.session.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

class AttributionIdentityLeaseTest {
    @Test fun revokedLeaseCannotReviveAfterNewOptInOrForegroundRun() = runTest {
        val service = readyService()
        val sessions = LeaseSessions()
        val old = assertNotNull(permittedIdentity(service, sessions))
        assertTrue(old.isCurrent())
        service.setConsent(AnalyticsConsent.Declined)
        service.setConsent(AnalyticsConsent.Accepted)
        assertFalse(old.isCurrent())
        val foreground = assertNotNull(permittedIdentity(service, sessions))
        service.setForeground(false)
        service.setForeground(true)
        service.restoreConsent()
        assertFalse(foreground.isCurrent())
        assertTrue(assertNotNull(permittedIdentity(service, sessions)).isCurrent())
    }

    @Test fun sessionSwitchStopsLeaseBeforeAsyncAnalyticsReconciliation() = runTest {
        val service = readyService()
        val sessions = LeaseSessions()
        val old = assertNotNull(permittedIdentity(service, sessions))
        sessions.current = Session("fake-other", userId = "other")
        sessions.generation++
        assertFalse(old.isCurrent())
    }

    private suspend fun TestScope.readyService(): AnalyticsService {
        val sdk = LeaseTransport()
        val service = AnalyticsService(AnalyticsContext("test", "1.0", "development"),
            MemoryConsent(AnalyticsConsent.Accepted), sdk, backgroundScope, true)
        service.setForeground(true)
        service.syncIdentity("recipient")
        return service
    }
}

private class LeaseTransport(private val recording: RecordingTransport = RecordingTransport()) : AnalyticsTransport by recording {
    private var account: String? = null
    override fun identify(userId: String) {
        account = userId
        recording.identify(userId)
    }
    override fun identity() = account?.let { it to "real-test-session" }
}

private class LeaseSessions : SessionReader {
    var current = Session("fake-access", userId = "recipient")
    var generation = 1L
    override fun accountGeneration() = generation
    override fun peekSession() = current
    override fun isActiveSession(session: Session?) = session != null
    override fun observeSession() = flowOf(current)
    override suspend fun getSession() = current
}
