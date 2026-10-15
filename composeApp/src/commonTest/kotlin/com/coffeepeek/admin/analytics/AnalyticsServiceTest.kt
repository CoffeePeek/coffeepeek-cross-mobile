@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import kotlinx.coroutines.test.*
import kotlinx.coroutines.launch
import kotlin.test.*

internal class MemoryConsent(var consent: AnalyticsConsent = AnalyticsConsent.Unknown) : AnalyticsConsentStore {
    var failWrite = false
    override suspend fun read() = consent
    override suspend fun write(consent: AnalyticsConsent) {
        if (failWrite) error("disk unavailable")
        this.consent = consent
    }
}

internal class RecordingTransport : AnalyticsTransport {
    val calls = mutableListOf<String>()
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val queue = mutableListOf<String>()
    var failStart = false
    var allow: () -> Boolean = { false }
    override fun start(allow: () -> Boolean, userId: String?) {
        if (failStart) error("SDK unavailable")
        this.allow = allow
        calls += "start"
    }
    override fun identify(userId: String) { calls += "identify:$userId" }
    override fun capture(event: String, properties: Map<String, Any>) {
        if (allow()) { events += event to properties; queue += event }
    }
    override fun screen(page: String, properties: Map<String, Any>) { if (allow()) events += "\$screen" to properties }
    override fun erase(resetIdentity: Boolean) { calls += if (resetIdentity) "reset" else "pause"; queue.clear() }
}

internal fun TestScope.analytics(store: MemoryConsent = MemoryConsent(), sdk: RecordingTransport = RecordingTransport(), enabled: Boolean = true): AnalyticsService =
    AnalyticsService(AnalyticsContext("coffee-peek-android", "1.0+1", "development"), store, sdk, backgroundScope, enabled)

class AnalyticsServiceTest {
    @Test fun acceptedConsentDoesNotStartSdkOrQueueGuestEventsBeforeLogin() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(sdk = sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.setConsent(AnalyticsConsent.Accepted)
        assertTrue(sdk.calls.isEmpty())
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        service.syncIdentity(null)
        service.screenShown("discovery", "guest-visit"); runCurrent()
        val guestOperation = service.operationScope()
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        assertTrue(sdk.calls.isEmpty())
        assertTrue(sdk.events.isEmpty())
        service.syncIdentity("A")
        assertEquals(listOf("start", "identify:A"), sdk.calls)
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in"), guestOperation))
        assertTrue(sdk.events.none { it.first == "auth_gate_shown" })
    }

    @Test fun debugRequiresExplicitEnableEvenAfterConsent() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk, enabled = false)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("backend-user")
        assertTrue(sdk.calls.isEmpty())
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
    }

    @Test fun accountSwitchAndLogoutNeverLinkAccountsOrRestartGuestCollection() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null)
        assertTrue(sdk.calls.isEmpty())
        service.syncIdentity("A")
        assertEquals(listOf("start", "identify:A"), sdk.calls)
        service.syncIdentity("B")
        assertEquals(listOf("start", "identify:A", "reset", "start", "identify:B"), sdk.calls)
        service.syncIdentity(null)
        assertEquals("reset", sdk.calls.last())
        val afterLogout = sdk.calls.toList()
        service.setForeground(false); service.setForeground(true); runCurrent()
        assertEquals(afterLogout, sdk.calls)
        assertFalse(service.state.value.ready)
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        assertTrue(sdk.events.none { it.first == "sign_in_completed" })
    }

    @Test fun screenDedupeAndEnvelopeCannotBeOverridden() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        service.screenShown("discovery", "visit-1"); runCurrent()
        service.screenShown("discovery", "visit-1"); runCurrent()
        assertEquals(1, sdk.events.count { it.first == "\$screen" })
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in", "app" to "forged")))
        assertTrue(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in"))); runCurrent()
        val properties = sdk.events.last().second
        assertEquals("coffee-peek-android", properties["app"])
        assertEquals("signed_in", properties["auth_state"])
        assertEquals("discovery", properties["page_type"])
    }

    @Test fun revokeInvalidatesQueuedEventsAndRejectsOldOperationScopes() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        val old = service.operationScope()
        repeat(50) { assertTrue(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in"))) }
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        service.setConsent(AnalyticsConsent.Declined)
        assertTrue(sdk.queue.isEmpty())
        assertTrue(sdk.events.isEmpty())
        service.setConsent(AnalyticsConsent.Accepted)
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in"), old))
        assertTrue(sdk.events.isEmpty())
    }

    @Test fun accountChangeAndBackgroundDiscardOfflineQueue() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        val old = service.operationScope()
        service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")); runCurrent()
        assertEquals(1, sdk.queue.size)
        service.setForeground(false); runCurrent()
        assertTrue(sdk.queue.isEmpty())
        assertEquals("pause", sdk.calls.last())
        service.setForeground(true); runCurrent()
        service.syncIdentity("B")
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in"), old))
    }

    @Test fun sdkAndConsentStorageFailuresStayOutsideProductCode() = runTest {
        val sdk = RecordingTransport().apply { failStart = true }
        val store = MemoryConsent()
        val service = analytics(store, sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        service.setConsent(AnalyticsConsent.Accepted)
        assertFalse(service.state.value.ready)
        sdk.failStart = false
        store.failWrite = true
        service.setConsent(AnalyticsConsent.Accepted)
        assertFalse(service.state.value.ready)
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
    }

    @Test fun closedSdkCallbackCannotBecomeValidForANewAccount() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        val previousSdkCallback = sdk.allow
        assertTrue(previousSdkCallback())
        service.syncIdentity("B")
        assertFalse(previousSdkCallback())
        assertTrue(sdk.allow())
    }

    @Test fun navigationReturnIsANewVisitButRotationAndForegroundAreNot() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        service.screenShown("discovery", "route-A"); runCurrent()
        service.screenShown("discovery", "route-A"); runCurrent()
        service.setForeground(false); service.setForeground(true); runCurrent()
        assertEquals(1, sdk.events.count { it.first == "\$screen" })
        service.screenShown("shop_detail", "route-B"); runCurrent()
        service.screenShown("discovery", "route-A"); runCurrent()
        assertEquals(3, sdk.events.count { it.first == "\$screen" })
    }

    @Test fun lateConsentWriteCannotOverrideRevocation() = runTest {
        val writing = kotlinx.coroutines.CompletableDeferred<Unit>()
        val resumeWrite = kotlinx.coroutines.CompletableDeferred<Unit>()
        var stored = AnalyticsConsent.Unknown
        val store = object : AnalyticsConsentStore {
            override suspend fun read() = stored
            override suspend fun write(consent: AnalyticsConsent) {
                if (consent == AnalyticsConsent.Accepted) { writing.complete(Unit); resumeWrite.await() }
                stored = consent
            }
        }
        val sdk = RecordingTransport()
        val service = AnalyticsService(AnalyticsContext("coffee-peek-ios", "1+1", "development"), store, sdk, backgroundScope, true)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity("A")
        val acceptance = launch { service.setConsent(AnalyticsConsent.Accepted) }
        writing.await()
        val revocation = launch { service.setConsent(AnalyticsConsent.Declined) }
        runCurrent()
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        resumeWrite.complete(Unit)
        acceptance.join(); revocation.join()
        assertEquals(AnalyticsConsent.Declined, stored)
        assertEquals(AnalyticsConsent.Declined, service.state.value.consent)
        assertTrue(sdk.calls.none { it == "start" })
    }
}
