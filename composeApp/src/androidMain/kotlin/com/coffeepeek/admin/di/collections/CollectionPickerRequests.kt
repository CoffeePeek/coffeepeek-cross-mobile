package com.coffeepeek.admin.di.collections

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** App-owned delivery of a successful heart action to the root sheet, never a favorite writer. */
internal class CollectionPickerRequests(private val sessions: SessionReader, private val scope: CoroutineScope) : CollectionPickerPrompts {
    internal data class Request(val lease: CollectionPickerLease, val target: CollectionTarget)
    private val lock = Any()
    private var owner = activeOwner()
    private var accountGeneration = sessions.accountGeneration()
    private var generation = 0L
    private var afterAuth: Pair<CollectionTarget, TimeMark>? = null
    private val pending = MutableStateFlow<Request?>(null)
    private var expiry: Job? = null
    val state = pending.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().collect {
                updateAccount()
                resumeAfterAuth()
            }
        }
    }

    /** Opens the same picker for selected and unselected hearts; guests resume it after authentication. */
    fun request(target: CollectionTarget): Boolean {
        if (target.slug.isBlank()) return true
        val active = activeOwner()
        if (active != null) {
            synchronized(lock) { afterAuth = null }
            added(lease(active), target)
            return true
        }
        synchronized(lock) { afterAuth = target to TimeSource.Monotonic.markNow() }
        if (activeOwner() != null) { resumeAfterAuth(); return true }
        return false
    }

    private fun resumeAfterAuth() {
        val active = activeOwner() ?: return
        val target = synchronized(lock) { afterAuth.also { afterAuth = null } }
            ?.takeIf { it.second.elapsedNow() < 10.minutes }?.first ?: return
        added(lease(active), target)
    }

    override fun lease(account: String): CollectionPickerLease? = synchronized(lock) {
        updateAccount()
        if (owner == account) CollectionPickerLease(account, generation) else null
    }

    override fun added(lease: CollectionPickerLease?, slug: String) = added(lease, CollectionTarget(CollectionTargetKind.CoffeeShop, slug))

    fun added(lease: CollectionPickerLease?, target: CollectionTarget): Unit = synchronized(lock) {
        updateAccount()
        if (lease == null || owner != lease.owner || generation != lease.generation || target.slug.isBlank()) return@synchronized
        if (pending.value?.let { it.lease == lease && it.target == target } == true) return@synchronized
        val request = Request(lease, target)
        pending.value = request
        expiry?.cancel()
        expiry = scope.launch {
            delay(60_000)
            dismiss(request)
        }
    }

    fun presented(request: Request) = synchronized(lock) {
        if (pending.value === request) expiry?.cancel()
    }

    fun dismiss(request: Request) = synchronized(lock) {
        if (pending.value === request) {
            pending.value = null
            expiry?.cancel()
        }
    }

    private fun updateAccount(current: String? = activeOwner()) = synchronized(lock) {
        val currentGeneration = sessions.accountGeneration()
        if (current != owner || currentGeneration != accountGeneration) {
            owner = current
            accountGeneration = currentGeneration
            generation++
            pending.value = null
            expiry?.cancel()
        }
    }

    private fun activeOwner() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
}
