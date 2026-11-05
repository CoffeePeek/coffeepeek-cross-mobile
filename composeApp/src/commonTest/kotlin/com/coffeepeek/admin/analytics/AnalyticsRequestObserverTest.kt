@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AnalyticsRequestObserverTest {
    @Test fun readerPreservesWirePropertiesWithoutCredentialLeak() = runTest {
        val fixture = requestFixture()
        val request = fixture.observer.begin("shops", "GET")!!
        request.complete(200, null, 17L, true)
        runCurrent()
        val properties = fixture.sdk.events.single { it.first == "api_request_completed" }.second
        assertEquals("shops", properties["category"])
        assertEquals("GET", properties["method"])
        assertEquals("success", properties["outcome"])
        assertEquals(17L, properties["duration_ms"])
        assertEquals(true, properties["auth_retried"])
        assertEquals(200, properties["status_code"])
        assertFalse(properties.toString().contains("private-token"))
    }

    @Test fun responseFromReplacedAccountIsDropped() = runTest {
        val fixture = requestFixture()
        val request = fixture.observer.begin("shops", "GET")!!
        fixture.sessions.state.value = Session("other-token", userId = "B")
        request.complete(200, null, 12L, false)
        runCurrent()
        assertTrue(fixture.sdk.events.isEmpty())
    }

    @Test fun revokedConsentDropsInFlightResponse() = runTest {
        val fixture = requestFixture()
        val request = fixture.observer.begin("shops", "GET")!!
        fixture.service.setConsent(AnalyticsConsent.Declined)
        request.complete(200, null, 12L, false)
        runCurrent()
        assertTrue(fixture.sdk.events.isEmpty())
    }

    @Test fun cancellationIsExcludedAndFailureIsSanitized() = runTest {
        val fixture = requestFixture()
        fixture.observer.begin("shops", "GET")!!.complete(null, CancellationException(), 1L, false)
        fixture.observer.begin("shops", "GET")!!.complete(403, IllegalStateException("private-token"), 3L, false)
        runCurrent()
        val properties = fixture.sdk.events.single().second
        assertEquals("failure", properties["outcome"])
        assertEquals("permission", properties["error_category"])
        assertEquals(403, properties["status_code"])
        assertFalse(properties.toString().contains("private-token"))
    }

    private suspend fun TestScope.requestFixture(): RequestFixture {
        val sdk = RecordingTransport()
        val sessions = ReadOnlySessions(Session("private-token", userId = "A"))
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("A")
        return RequestFixture(sdk, sessions, service, analyticsRequestObserver(service, sessions))
    }

    private data class RequestFixture(
        val sdk: RecordingTransport,
        val sessions: ReadOnlySessions,
        val service: AnalyticsService,
        val observer: com.coffeepeek.core.network.RequestObserver,
    )
}
