package com.coffeepeek.feature.ownership.impl.ui

import com.coffeepeek.feature.ownership.api.BusinessSessionState
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class OwnershipAccess(private val repository: BusinessOwnershipRepository, private val account: OwnershipAccount) {
    val state = MutableStateFlow(BusinessSessionState())
    private var identity: String? = null
    private var businesses: Map<Pair<BusinessKind, String>, String> = emptyMap()
    private val mutex = Mutex()
    private var switching = false
    private var resetEpoch = 0L

    fun reset() {
        resetEpoch++
        identity = account.current()
        businesses = emptyMap()
        state.value = BusinessSessionState()
    }
    fun owns(kind: BusinessKind, id: String): Boolean =
        identity != null && identity == account.current() && kind to id in businesses

    fun editorId(kind: BusinessKind, id: String, slug: String): String? =
        if (identity == null || identity != account.current()) null else businesses[kind to slug] ?: businesses[kind to id]

    suspend fun refresh() = mutex.withLock { refreshLocked() }
    suspend fun switchMode() {
        if (switching) return
        val requestedOwner = account.current()
        val requestedEpoch = resetEpoch
        val nextBusinessMode = !state.value.businessMode
        switching = true
        try {
            // The flag write must not interleave with a concurrent refresh: refresh() republishes
            // the whole state from the value it read before the flip and would silently undo it.
            mutex.withLock {
                if (!isCurrent(requestedOwner, requestedEpoch)) return@withLock
                if (!nextBusinessMode) state.value = state.value.copy(businessMode = false)
                else {
                    if (requestedOwner != identity) reset()
                    val publicationEpoch = resetEpoch
                    refreshLocked()
                    if (isCurrent(requestedOwner, publicationEpoch) && state.value.hasBusinesses) {
                        state.value = state.value.copy(businessMode = true)
                    }
                }
            }
        } finally { switching = false }
    }
    /** Caller holds the mutex; refresh() and switchMode() serialize around the network reads. */
    private suspend fun refreshLocked() {
        val owner = account.current()
        if (owner != identity) reset()
        if (owner == null) return
        val requestEpoch = resetEpoch
        try {
            val owned = BusinessKind.entries.flatMap { loadAll(it) }.toMap()
            if (!isCurrent(owner, requestEpoch)) {
                if (owner != account.current()) reset()
                return
            }
            businesses = owned
            state.value = BusinessSessionState(state.value.businessMode && owned.isNotEmpty(), owned.isNotEmpty(), owned.keys.map { "${it.first.name}:${it.second}" }.toSet())
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // A transient failure must not silently kick the owner out of business mode;
            // the last known session stays until the next successful refresh.
        }
    }
    private fun isCurrent(owner: String?, epoch: Long) = owner == account.current() && epoch == resetEpoch
    private suspend fun loadAll(kind: BusinessKind): List<Pair<Pair<BusinessKind, String>, String>> {
        val result = mutableListOf<Pair<Pair<BusinessKind, String>, String>>()
        var page = 1
        do {
            val response = repository.businesses(kind, page++).getOrThrow()
            result.addAll(response.items.flatMap { business ->
                listOfNotNull((kind to business.id) to business.id, business.slug?.let { (kind to it) to business.id })
            })
        } while (response.hasNext)
        return result
    }
}
