@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AnalyticsSessionObservationTest {
    @Test fun readerTransitionsKeepBackendIdentityAndRevokeOnLogout() = runTest {
        val sessions = ReadOnlySessions(Session("private-token", userId = "A"))
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        backgroundScope.launch { observeAnalyticsSession(service, sessions) }
        runCurrent()
        assertEquals(listOf("start", "identify:A"), sdk.calls)
        sessions.state.value = Session("other-token", userId = "B")
        runCurrent()
        assertEquals("identify:B", sdk.calls.last())
        sessions.state.value = null
        runCurrent()
        assertEquals("reset", sdk.calls.last())
        assertFalse(service.state.value.ready)
        assertTrue(sdk.calls.none { "token" in it })
    }

    @Test fun activeCredentialsWithoutBackendUserNeverBecomeAnalyticsIdentity() = runTest {
        val sessions = ReadOnlySessions(Session("secret-token"))
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        backgroundScope.launch { observeAnalyticsSession(service, sessions) }
        runCurrent()
        assertFalse(service.state.value.authReady)
        assertFalse(service.state.value.ready)
        assertTrue(sdk.calls.isEmpty())
    }

    @Test fun declinedConsentPreventsSdkStartWithAuthenticatedReader() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Declined), sdk)
        service.setForeground(true)
        backgroundScope.launch { observeAnalyticsSession(service, ReadOnlySessions(Session("token", userId = "A"))) }
        runCurrent()
        assertFalse(service.state.value.ready)
        assertTrue(sdk.calls.isEmpty())
    }

    @Test fun readFailureRevokesExistingAnalyticsIdentity() = runTest {
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted))
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("A")
        val sessions = object : ReadOnlySessions() {
            override suspend fun getSession(): Session? = error("Secure storage unavailable")
        }
        observeAnalyticsSession(service, sessions)
        assertFalse(service.state.value.authReady)
        assertFalse(service.state.value.ready)
    }

    @Test fun observerFailureRevokesExistingAnalyticsIdentity() = runTest {
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted))
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("A")
        val sessions = object : ReadOnlySessions() {
            override fun observeSession() = flow<Session?> { error("Observation unavailable") }
        }
        observeAnalyticsSession(service, sessions)
        assertFalse(service.state.value.authReady)
        assertFalse(service.state.value.ready)
    }

    @Test fun cancelledLoadIsRethrown() = runTest {
        val cancellation = CancellationException("Owner closed")
        val sessions = object : ReadOnlySessions() {
            override suspend fun getSession(): Session? = throw cancellation
        }
        val service = analytics()
        assertSame(cancellation, assertFailsWith<CancellationException> { observeAnalyticsSession(service, sessions) })
    }
}
