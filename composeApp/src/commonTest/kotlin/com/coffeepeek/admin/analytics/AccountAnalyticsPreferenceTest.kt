@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

internal class AccountConsentStore : AnalyticsConsentStore {
    val accounts = mutableMapOf("A" to AnalyticsConsent.Accepted, "B" to AnalyticsConsent.Declined)
    var gate: CompletableDeferred<Unit>? = null
    var failRead = false
    var failWrite = false
    override suspend fun read(userId: String?): AnalyticsConsent {
        val value = accounts[userId] ?: AnalyticsConsent.Unknown
        if (userId != null) gate?.await()
        check(!failRead)
        return value
    }
    override suspend fun write(userId: String?, consent: AnalyticsConsent) {
        check(!failWrite)
        if (userId != null) accounts[userId] = consent
    }
}

class AccountAnalyticsPreferenceTest {
    private fun TestScope.service(store: AccountConsentStore, sdk: RecordingTransport) =
        AnalyticsService(AnalyticsContext("test", "test", "test"), store, sdk, backgroundScope, true)

    @Test fun aNewInstallationRestoresServerRefusalAndKeepsAccountsSeparate() = runTest {
        val store = AccountConsentStore(); val sdk = RecordingTransport()
        val service = service(store, sdk)
        service.setForeground(true); service.syncIdentity("B")
        assertEquals(AnalyticsConsent.Declined, service.state.value.consent)
        assertTrue(sdk.calls.isEmpty())
        service.syncIdentity("A")
        assertTrue(service.state.value.ready)
        service.syncIdentity("B")
        assertFalse(service.state.value.ready)
        assertFalse(sdk.allow())
    }

    @Test fun signInIsCapturedOnceOnlyAfterServerChoiceHasLoaded() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = AccountConsentStore().apply { this.gate = gate }
        val sdk = RecordingTransport(); val service = service(store, sdk)
        service.setForeground(true); service.signedIn("A", "google", "login", null); runCurrent()
        assertTrue(sdk.events.isEmpty())
        assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
        gate.complete(Unit); runCurrent()
        assertEquals(1, sdk.events.count { it.first == "sign_in_completed" })
        assertEquals(listOf("start", "identify:A"), sdk.calls)
    }

    @Test fun lateOldAccountReadCannotEnableTheReplacementAccount() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = AccountConsentStore().apply { this.gate = gate }
        val sdk = RecordingTransport(); val service = service(store, sdk)
        service.setForeground(true); service.syncIdentityNow("A"); runCurrent()
        service.syncIdentityNow("B"); gate.complete(Unit); runCurrent()
        assertEquals("B", service.state.value.userId)
        assertEquals(AnalyticsConsent.Declined, service.state.value.consent)
        assertFalse(service.state.value.ready)
        assertTrue(sdk.events.isEmpty())
    }

    @Test fun failedLoadStaysOffUntilForegroundRetrySucceeds() = runTest {
        val store = AccountConsentStore().apply { failRead = true }
        val sdk = RecordingTransport(); val service = service(store, sdk)
        service.setForeground(true); service.syncIdentity("A")
        assertFalse(service.state.value.ready)
        assertTrue(service.state.value.preferenceSyncFailed)
        store.failRead = false
        service.setForeground(false); service.setForeground(true); runCurrent()
        assertTrue(service.state.value.ready)
        assertFalse(service.state.value.preferenceSyncFailed)
    }

    @Test fun failedDisableRemainsOffAndRetriesTheChoiceBeforeReadingServerDefault() = runTest {
        val store = AccountConsentStore(); val sdk = RecordingTransport()
        val service = service(store, sdk)
        service.setForeground(true); service.syncIdentity("A")
        store.failWrite = true; service.setConsent(AnalyticsConsent.Declined)
        assertFalse(service.state.value.ready)
        assertFalse(sdk.allow())
        assertTrue(service.state.value.preferenceSyncFailed)
        store.failWrite = false
        service.setForeground(false); service.setForeground(true); runCurrent()
        assertEquals(AnalyticsConsent.Declined, store.accounts["A"])
        assertFalse(service.state.value.ready)
        assertFalse(service.state.value.preferenceSyncFailed)
    }

    @Test fun anotherDeviceRefusalIsReloadedOnForeground() = runTest {
        val store = AccountConsentStore(); val sdk = RecordingTransport()
        val service = service(store, sdk)
        service.setForeground(true); service.syncIdentity("A")
        service.setForeground(false); runCurrent()
        store.accounts["A"] = AnalyticsConsent.Declined
        service.setForeground(true); runCurrent()
        assertFalse(service.state.value.ready)
        assertEquals(AnalyticsConsent.Declined, service.state.value.consent)
    }
}
