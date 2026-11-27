package com.coffeepeek.feature.attribution.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Owns permitted observations, not SDK capture or a second offline event queue. */
class AttributionEngine(
    private val backend: AttributionBackend,
    private val store: AttributionStore,
    private val clock: AttributionClock,
    private val installChannel: String,
    private val installSource: AttributionInstallSource = AttributionInstallSource { null },
) {
    private val lock = Mutex()
    private var state: AttributionLocalState? = null
    private var identity: AttributionIdentity? = null
    private var entry: AttributionEntry = AttributionEntry.Unknown
    private var expectedPath: String? = null
    private var visiblePath: String? = null
    private var observation: AttributionObservation? = null
    private var observed = false
    private var acceptedObservation: String? = null
    private var claimKey: String? = null
    private var installationKey: String? = null
    private var suppressed = false
    private var lastOpen: Pair<String, Long>? = null
    private var lastPath: String? = null
    private var recoveryEvidence: AttributionEvidence? = null

    suspend fun open(code: String): String = lock.withLock {
        require(validAttributionCode(code))
        if (lastOpen?.let { it.first == code && clock.now() - it.second in 0..5_000 } == true)
            return@withLock requireNotNull(lastPath)
        val resolved = backend.resolve(code)
        currentCoroutineContext().ensureActive()
        require(validCanonicalAttributionPath(resolved.canonicalPath))
        remember(AttributionEntry.Link(code), resolved.canonicalPath)
        suppressed = !resolved.trackingEnabled
        lastOpen = code to clock.now()
        lastPath = resolved.canonicalPath
        resolved.canonicalPath
    }

    suspend fun recoverHandoff(token: String, transport: String) = lock.withLock {
        require(token.length in 1..4096 && token.none { it.isISOControl() })
        require(transport in setOf("play_referrer", "reopened_link"))
        remember(AttributionEntry.Handoff(token, transport), null)
        process()
    }

    suspend fun visible(path: String) = lock.withLock {
        if (!validCanonicalAttributionPath(path)) return@withLock
        visiblePath = path
        process()
    }

    suspend fun sync(next: AttributionIdentity) = lock.withLock {
        require(next.accountId.isNotBlank() && next.sdkDistinctId == next.accountId)
        require(next.platform in setOf("android", "ios"))
        if (identity?.accountId?.let { it != next.accountId } == true) clearLocked(false, true)
        identity = next
        load()
        if (state?.evidence?.accountId?.let { it != next.accountId } == true) clearLocked(false, false)
        retryRevocations()
        process()
    }

    suspend fun pause() = lock.withLock { identity = null }

    suspend fun clearAccount(resetInstallation: Boolean) = lock.withLock {
        identity = null
        clearLocked(resetInstallation, true)
        retryRevocations()
    }

    suspend fun share(path: String, placement: String): String = lock.withLock {
        require(validCanonicalAttributionPath(path))
        require(placement.matches(Regex("[a-z][a-z0-9_]{0,63}")))
        val observer = identity?.takeIf { it.isCurrent() }?.let { currentEvidence(it)?.observerToken }
        backend.share(path, placement, clock.uuid(), observer)
    }

    private fun remember(next: AttributionEntry, path: String?) {
        if (entry == next && expectedPath == path && !observed) return
        resetEntry(next, path)
    }

    private fun resetEntry(next: AttributionEntry, path: String?) {
        entry = next
        expectedPath = path
        visiblePath = null
        observation = null
        claimKey = null
        observed = false
        acceptedObservation = null
        recoveryEvidence = null
        suppressed = false
    }

    private suspend fun load() {
        if (state != null) return
        val loaded = store.load()
        state = loaded.copy(revocations = validRevocations(loaded.revocations))
    }

    private suspend fun process() {
        val permit = identity?.takeIf { it.isCurrent() } ?: return
        if (suppressed || visiblePath == null || expectedPath?.let { it != visiblePath } == true) return
        load()
        if (!backend.enabled() || !permit.isCurrent()) return
        try {
            readInstallSource(permit)
            observe(permit)
            reportInstallation(permit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: AttributionFailure) {
            reject(error)
            throw error
        }
    }

    private suspend fun reject(error: AttributionFailure) {
        when {
            error.code in setOf("OBSERVER_EXPIRED", "OBSERVER_INVALID", "ATTRIBUTION_RECEIPT_EXPIRED") -> expireObserver()
            error.status == 403 || error.code in setOf("OBSERVER_REVOKED", "OBSERVER_ACCOUNT_MISMATCH", "ATTRIBUTION_ALREADY_CLAIMED") -> {
                identity = null
                clearLocked(error.status == 403, true)
            }
            error.status in 400..499 && error.status != 429 -> suppressed = true
        }
    }

    private suspend fun observe(permit: AttributionIdentity) {
        prepareRecovery(permit)
        if (observed) return
        val pending = observation ?: AttributionObservation(clock.uuid(), entry, permit).also { observation = it }
        val evidence = if (acceptedObservation == pending.id) requireNotNull(state?.evidence)
            else acceptTouch(pending, permit)
        if (!permit.isCurrent()) return
        attach(evidence, permit)
        observed = true
    }

    private suspend fun acceptTouch(pending: AttributionObservation, permit: AttributionIdentity): AttributionEvidence {
        val fresh = backend.touch(pending, currentEvidence(permit)?.observerToken)
        val evidence = recoveryEvidence?.let { fresh.copy(receipt = it.receipt, receiptExpiresAt = it.receiptExpiresAt) } ?: fresh
        if (!permit.isCurrent()) {
            retainLateRevocation(evidence.observerToken)
            return evidence
        }
        state = requireNotNull(state).copy(evidence = evidence)
        store.save(requireNotNull(state))
        acceptedObservation = pending.id
        return evidence
    }

    private fun prepareRecovery(permit: AttributionIdentity) {
        val stale = state?.evidence?.takeIf { it.accountId == permit.accountId && it.observerExpiresAt <= clock.now() } ?: return
        if (observed && state?.installationConfirmed == true) return
        if (entry != AttributionEntry.Unknown && acceptedObservation == null) return
        recoveryEvidence = stale.takeIf { it.receiptExpiresAt > clock.now() }
        observation = null
        acceptedObservation = null
        claimKey = null
        observed = false
        entry = AttributionEntry.Unknown
    }

    private suspend fun readInstallSource(permit: AttributionIdentity) {
        val current = requireNotNull(state)
        if (current.referrerRead || !permit.isCurrent()) return
        val token = installSource.read()
        if (!permit.isCurrent()) return
        if (token != null && entry == AttributionEntry.Unknown) entry = AttributionEntry.Handoff(token, "play_referrer")
        state = current.copy(referrerRead = true)
        store.save(requireNotNull(state))
    }

    private suspend fun attach(evidence: AttributionEvidence, permit: AttributionIdentity) {
        if (!permit.isCurrent()) return
        val key = claimKey ?: clock.uuid().also { claimKey = it }
        backend.claim(evidence, permit, key)
        recoveryEvidence = null
    }

    private suspend fun reportInstallation(permit: AttributionIdentity) {
        val current = requireNotNull(state)
        if (current.installationConfirmed || !permit.isCurrent()) return
        val evidence = currentEvidence(permit) ?: return
        val id = current.installationId ?: clock.uuid().also {
            state = current.copy(installationId = it)
            store.save(requireNotNull(state))
        }
        val key = installationKey ?: clock.uuid().also { installationKey = it }
        backend.installation(id, installChannel, evidence, permit, key)
        if (!permit.isCurrent()) return
        state = requireNotNull(state).copy(installationConfirmed = true)
        store.save(requireNotNull(state))
    }

    private fun currentEvidence(permit: AttributionIdentity): AttributionEvidence? = state?.evidence?.takeIf {
        it.accountId == permit.accountId && it.receiptExpiresAt > clock.now() && it.observerExpiresAt > clock.now()
    }

    private fun expireObserver() {
        state = state?.copy(evidence = null)
        recoveryEvidence = null
        observation = null
        claimKey = null
        observed = false
        acceptedObservation = null
        entry = AttributionEntry.Unknown
        expectedPath = null
    }

    private suspend fun clearLocked(resetInstallation: Boolean, clearFunctional: Boolean) {
        val loadedPath = visiblePath
        load()
        recoveryEvidence = null
        val current = requireNotNull(state)
        val revocations = current.evidence?.let {
            current.revocations + ObserverRevocation(it.observerToken, clock.now() + WEEK)
        } ?: current.revocations
        state = current.copy(evidence = null, revocations = validRevocations(revocations))
        if (resetInstallation) state = requireNotNull(state).copy(installationId = null, installationConfirmed = false, referrerRead = false)
        observation = null
        claimKey = null
        installationKey = null
        if (clearFunctional) {
            resetEntry(AttributionEntry.Unknown, null)
            lastOpen = null
            lastPath = null
        }
        if (resetInstallation) visiblePath = loadedPath
        store.save(requireNotNull(state))
    }

    private suspend fun retainLateRevocation(token: String) = withContext(NonCancellable) {
        val current = requireNotNull(state)
        state = current.copy(revocations = validRevocations(current.revocations + ObserverRevocation(token, clock.now() + WEEK)))
        store.save(requireNotNull(state))
    }

    private suspend fun retryRevocations() {
        val current = requireNotNull(state)
        for (credential in validRevocations(current.revocations)) revokeOne(credential)
        state = requireNotNull(state).copy(revocations = validRevocations(requireNotNull(state).revocations))
        store.save(requireNotNull(state))
    }

    private suspend fun revokeOne(credential: ObserverRevocation) {
        try {
            backend.revoke(credential.token)
            removeRevocation(credential.token)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: AttributionFailure) {
            if (error.status == 401) removeRevocation(credential.token)
        } catch (_: Exception) {
            // Retain only the bounded control credential, never an observation payload.
        }
    }

    private fun removeRevocation(token: String) {
        state = state?.let { it.copy(revocations = it.revocations.filterNot { item -> item.token == token }) }
    }

    private fun validRevocations(values: List<ObserverRevocation>) = values
        .filter { it.expiresAt > clock.now() && it.expiresAt <= clock.now() + WEEK }
        .distinctBy { it.token }
        .takeLast(20)

    private companion object { const val WEEK = 7 * 24 * 60 * 60 * 1000L }
}
