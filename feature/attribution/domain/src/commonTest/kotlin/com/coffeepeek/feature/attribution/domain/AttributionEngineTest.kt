package com.coffeepeek.feature.attribution.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AttributionEngineTest {
    @Test fun authoritativeDenialFromVisibleStopsDeliveryAndErasesAccountEvidence() = runTest {
        val fixture = Fixture()
        fixture.engine.sync(fixture.permit())
        fixture.backend.touchFailure = AttributionFailure(403, "ANALYTICS_NOT_PERMITTED")
        assertFailsWith<AttributionFailure> { fixture.engine.visible("/") }
        assertEquals(1, fixture.permissionDenials)
        assertFalse(fixture.current)
        assertNull(fixture.store.state.evidence)
        assertNull(fixture.store.state.installationId)
        fixture.engine.visible("/")
        assertEquals(1, fixture.backend.touchAttempts.size)
    }

    @Test fun lateObserverQueuedForRevokeIsNotReusedAfterForegroundRecovery() = runTest {
        val fixture = Fixture()
        fixture.backend.afterTouch = { fixture.current = false }
        fixture.ready()
        fixture.engine.sync(fixture.permit().copy(isCurrent = { true }, sessionId = "new-session"))
        assertEquals(2, fixture.backend.touchAttempts.map { it.id }.distinct().size)
        assertEquals(listOf("observer-1"), fixture.backend.revoked)
        assertEquals("observer-2", fixture.store.state.evidence?.observerToken)
        assertTrue(fixture.store.state.installationConfirmed)
    }

    @Test fun touchRetryKeepsOriginalSdkSessionWithFreshPermission() = runTest { retrySession("touch") }
    @Test fun claimRetryKeepsOriginalSdkSessionWithFreshPermission() = runTest { retrySession("claim") }
    @Test fun installationRetryKeepsOriginalSdkSessionWithFreshPermission() = runTest { retrySession("installation") }

    private suspend fun retrySession(stage: String) {
        val fixture = Fixture()
        fixture.backend.touchFailure = if (stage == "touch") AttributionFailure(503, null) else null
        fixture.backend.failClaim = stage == "claim"
        fixture.backend.failInstallation = stage == "installation"
        assertFailsWith<AttributionFailure> { fixture.ready() }
        fixture.engine.pause()
        fixture.current = false
        fixture.engine.sync(fixture.permit().copy(sessionId = "new-session", isCurrent = { true }))
        val sessions = when (stage) {
            "touch" -> fixture.backend.touchAttempts.map { it.identity.sessionId }
            "claim" -> fixture.backend.claimSessions
            else -> fixture.backend.installationSessions
        }
        assertEquals(listOf("sdk-session", "sdk-session"), sessions)
        assertTrue(fixture.store.state.installationConfirmed)
    }

    @Test fun installationRetryKeepsFirstReceiptWhenAnotherLinkIsOpened() = runTest {
        val fixture = Fixture()
        fixture.backend.failInstallation = true
        assertFailsWith<AttributionFailure> { fixture.ready() }
        fixture.engine.open(CODE)
        fixture.engine.visible("/coffees/test")
        assertEquals(listOf("receipt-1", "receipt-1"), fixture.backend.installationReceipts)
        assertEquals(1, fixture.backend.installations.distinct().size)
    }

    @Test fun expiredReceiptIsErasedEvenWhileCollectionIsPaused() = runTest {
        val fixture = Fixture()
        fixture.backend.failRevoke = true
        fixture.store.state = AttributionLocalState(evidence = AttributionEvidence("expired-observer", fixture.time - DAY,
            "expired-receipt", fixture.time - 1, "recipient"))
        fixture.engine.pause()
        assertNull(fixture.store.state.evidence)
        assertEquals(1, fixture.store.state.revocations.size)
        assertTrue(fixture.backend.observations.isEmpty())
    }

    @Test fun serverExpiredObserverStartsFreshObservationWithoutOldCredential() = runTest {
        val fixture = Fixture()
        fixture.ready()
        fixture.engine.open(CODE)
        fixture.backend.touchFailure = AttributionFailure(401, "OBSERVER_EXPIRED")
        assertFailsWith<AttributionFailure> { fixture.engine.visible("/coffees/test") }
        fixture.engine.sync(fixture.permit())
        assertNull(fixture.backend.observers.last())
        assertEquals(AttributionEntry.Unknown, fixture.backend.observations.last().entry)
    }

    @Test fun expiredLeaseRecoversValidReceiptThroughNewPermittedObserver() = runTest {
        val fixture = Fixture()
        fixture.store.state = AttributionLocalState(evidence = AttributionEvidence("old-observer", fixture.time - 1,
            "recover-receipt", fixture.time + DAY, "recipient"))
        fixture.ready()
        assertEquals("recover-receipt", fixture.backend.claimedReceipts.single())
        assertEquals("recover-receipt", fixture.store.state.evidence?.receipt)
        assertTrue(fixture.backend.revoked.isEmpty())
    }

    @Test fun resolverAndGuestNeverCollect() = runTest {
        val fixture = Fixture()
        assertEquals("/coffees/test", fixture.engine.open(CODE))
        fixture.engine.visible("/coffees/test")
        assertEquals(1, fixture.backend.resolves)
        assertTrue(fixture.backend.observations.isEmpty())
        assertNull(fixture.store.state.installationId)
    }

    @Test fun permissionAndActualDestinationAreRequired() = runTest {
        val fixture = Fixture()
        fixture.engine.open(CODE)
        fixture.engine.sync(fixture.permit())
        fixture.engine.visible("/coffees/other")
        assertTrue(fixture.backend.observations.isEmpty())
        fixture.engine.visible("/coffees/test")
        assertEquals(AttributionEntry.Link(CODE), fixture.backend.observations.single().entry)
        assertEquals(1, fixture.backend.installations.size)
    }

    @Test fun disabledCapabilitiesDoNotCreateLocalIdentity() = runTest {
        val fixture = Fixture()
        fixture.backend.enabled = false
        fixture.ready()
        assertTrue(fixture.backend.observations.isEmpty())
        assertNull(fixture.store.state.installationId)
        assertEquals(0, fixture.sourceReads)
    }

    @Test fun duplicateVisibilityAndForegroundDoNotDuplicateInstallation() = runTest {
        val fixture = Fixture()
        fixture.ready()
        fixture.engine.visible("/")
        fixture.engine.pause()
        fixture.engine.sync(fixture.permit())
        assertEquals(1, fixture.backend.observations.size)
        assertEquals(1, fixture.backend.installations.size)
    }

    @Test fun claimRetryDoesNotResendTouchWithDifferentObserver() = runTest {
        val fixture = Fixture()
        fixture.backend.failClaim = true
        assertFailsWith<AttributionFailure> { fixture.ready() }
        fixture.engine.sync(fixture.permit())
        assertEquals(1, fixture.backend.observations.size)
        assertEquals(2, fixture.backend.claimKeys.size)
        assertEquals(1, fixture.backend.claimKeys.distinct().size)
        assertEquals(1, fixture.backend.installations.size)
    }

    @Test fun installationRetryKeepsIdAndKey() = runTest {
        val fixture = Fixture()
        fixture.backend.failInstallation = true
        assertFailsWith<AttributionFailure> { fixture.ready() }
        fixture.engine.sync(fixture.permit())
        assertEquals(2, fixture.backend.installations.size)
        assertEquals(1, fixture.backend.installations.distinct().size)
        assertEquals(1, fixture.backend.installationKeys.distinct().size)
    }

    @Test fun deniedPermissionDuringRequestRetainsOnlyRevocation() = runTest {
        val fixture = Fixture()
        fixture.backend.afterTouch = { fixture.current = false }
        fixture.ready()
        assertNull(fixture.store.state.evidence)
        assertNull(fixture.store.state.installationId)
        assertEquals(1, fixture.store.state.revocations.size)
        assertTrue(fixture.backend.claimKeys.isEmpty())
    }

    @Test fun revokeClearsIdentityAndPendingEntryBeforeNetworkRetry() = runTest {
        val fixture = Fixture()
        fixture.ready()
        fixture.backend.failRevoke = true
        fixture.engine.clearAccount(true)
        assertNull(fixture.store.state.evidence)
        assertNull(fixture.store.state.installationId)
        assertFalse(fixture.store.state.installationConfirmed)
        assertEquals(1, fixture.store.state.revocations.size)
        fixture.time += 8 * DAY
        fixture.engine.clearAccount(false)
        assertTrue(fixture.store.state.revocations.isEmpty())
    }

    @Test fun accountSwitchPreservesInstallationButErasesOldSource() = runTest {
        val fixture = Fixture()
        fixture.ready()
        val installation = fixture.store.state.installationId
        fixture.engine.sync(fixture.permit("other"))
        fixture.engine.visible("/")
        assertEquals(installation, fixture.store.state.installationId)
        assertEquals(1, fixture.backend.installations.size)
        assertEquals("other", fixture.backend.observations.last().identity.accountId)
        assertEquals(AttributionEntry.Unknown, fixture.backend.observations.last().entry)
        assertEquals(1, fixture.backend.revoked.size)
    }

    @Test fun playReferrerIsReadOnceAfterPermissionAndNeverOverridesOpenedLink() = runTest {
        val fixture = Fixture()
        fixture.source = "handoff"
        fixture.engine.open(CODE)
        fixture.engine.visible("/coffees/test")
        assertEquals(0, fixture.sourceReads)
        fixture.engine.sync(fixture.permit())
        assertEquals(1, fixture.sourceReads)
        assertEquals(AttributionEntry.Link(CODE), fixture.backend.observations.single().entry)
    }

    @Test fun nativeUnknownAndHandoffRemainDifferent() = runTest {
        val fixture = Fixture()
        fixture.source = "handoff"
        fixture.ready()
        assertEquals(AttributionEntry.Handoff("handoff", "play_referrer"), fixture.backend.observations.single().entry)
    }

    @Test fun cancellationPassesThrough() = runTest {
        val fixture = Fixture()
        fixture.backend.cancelTouch = true
        assertFailsWith<CancellationException> { fixture.ready() }
        assertNull(fixture.store.state.installationId)
    }

    @Test fun shareWorksWithoutAnalyticsObserver() = runTest {
        val fixture = Fixture()
        assertEquals("https://coffeepeek.by/l/$CODE", fixture.engine.share("/", "settings"))
        assertNull(fixture.backend.shareObserver)
        assertTrue(fixture.backend.observations.isEmpty())
    }

    @Test fun mismatchedSdkAndUntrustedPathsAreRejected() = runTest {
        val fixture = Fixture()
        assertFailsWith<IllegalArgumentException> { fixture.engine.sync(fixture.permit().copy(sdkDistinctId = "sender")) }
        assertFailsWith<IllegalArgumentException> { fixture.engine.open("../bad") }
        assertFalse(validCanonicalAttributionPath("//attacker.example"))
        assertFalse(validCanonicalAttributionPath("/l/$CODE"))
        assertFalse(validCanonicalAttributionPath("/coffees/test?token=private"))
    }
}

private class Fixture {
    var time = 1_800_000_000_000L
    var sequence = 0
    var current = true
    var source: String? = null
    var sourceReads = 0
    var permissionDenials = 0
    val store = MemoryStore()
    val backend = FakeBackend { time }
    val engine = AttributionEngine(backend, store, AttributionClock({ time }, { "id-${++sequence}" }), "unknown",
        AttributionInstallSource { sourceReads++; source }, { permissionDenials++; current = false })
    fun permit(account: String = "recipient") = AttributionIdentity(account, account, "sdk-session", "android", "2.0.0", "development") { current }
    suspend fun ready() {
        engine.visible("/")
        engine.sync(permit())
    }
}

private class MemoryStore : AttributionStore {
    var state = AttributionLocalState()
    override suspend fun load() = state
    override suspend fun save(state: AttributionLocalState) { this.state = state }
}

private class FakeBackend(private val now: () -> Long) : AttributionBackend {
    var enabled = true
    var resolves = 0
    var failClaim = false
    var failInstallation = false
    var failRevoke = false
    var cancelTouch = false
    var afterTouch: () -> Unit = {}
    var touchFailure: AttributionFailure? = null
    var shareObserver: String? = null
    val observations = mutableListOf<AttributionObservation>()
    val touchAttempts = mutableListOf<AttributionObservation>()
    val observers = mutableListOf<String?>()
    val claimKeys = mutableListOf<String>()
    val claimedReceipts = mutableListOf<String>()
    val claimSessions = mutableListOf<String?>()
    val installations = mutableListOf<String>()
    val installationKeys = mutableListOf<String>()
    val installationSessions = mutableListOf<String?>()
    val installationReceipts = mutableListOf<String>()
    val revoked = mutableListOf<String>()
    override suspend fun enabled() = enabled
    override suspend fun resolve(code: String): AttributionResolution {
        resolves++
        return AttributionResolution("/coffees/test", true)
    }
    override suspend fun touch(observation: AttributionObservation, observer: String?): AttributionEvidence {
        check(observation.identity.isCurrent())
        touchAttempts += observation
        if (cancelTouch) throw CancellationException()
        touchFailure?.let { touchFailure = null; throw it }
        observations += observation
        observers += observer
        afterTouch()
        return AttributionEvidence("observer-${observations.size}", now() + DAY, "receipt-${observations.size}", now() + 30 * DAY, observation.identity.accountId)
    }
    override suspend fun claim(evidence: AttributionEvidence, identity: AttributionIdentity, key: String) {
        check(identity.isCurrent())
        claimSessions += identity.sessionId
        claimKeys += key
        claimedReceipts += evidence.receipt
        if (failClaim) { failClaim = false; throw AttributionFailure(503, null) }
    }
    override suspend fun installation(id: String, channel: String, evidence: AttributionEvidence, identity: AttributionIdentity, key: String) {
        check(identity.isCurrent())
        installationSessions += identity.sessionId
        installationReceipts += evidence.receipt
        installations += id
        installationKeys += key
        if (failInstallation) { failInstallation = false; throw AttributionFailure(503, null) }
    }
    override suspend fun revoke(token: String) {
        if (failRevoke) throw AttributionFailure(503, null)
        revoked += token
    }
    override suspend fun share(canonicalPath: String, placement: String, key: String, observer: String?): String {
        shareObserver = observer
        return "https://coffeepeek.by/l/$CODE"
    }
}

private const val CODE = "abcdefghijklmnop"
private const val DAY = 86_400_000L
