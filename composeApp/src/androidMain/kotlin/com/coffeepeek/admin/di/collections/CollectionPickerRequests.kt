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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** App-owned delivery of a successful heart action to the root sheet, never a favorite writer. */
internal class CollectionPickerRequests(private val sessions: SessionReader, private val scope: CoroutineScope) : CollectionPickerPrompts {
    internal data class Request(val lease: CollectionPickerLease, val target: CollectionTarget)
    private val lock = Any()
    private var owner = activeOwner()
    private var generation = 0L
    private val pending = MutableStateFlow<Request?>(null)
    private var expiry: Job? = null
    val state = pending.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank) }
                .distinctUntilChanged().collect { updateAccount(it) }
        }
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
        if (current != owner) {
            owner = current
            generation++
            pending.value = null
            expiry?.cancel()
        }
    }

    private fun activeOwner() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
}
