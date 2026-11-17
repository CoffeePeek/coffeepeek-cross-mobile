package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityFeedActions
import com.coffeepeek.feature.community.api.CommunityFeedPresentation
import com.coffeepeek.feature.community.api.CommunityCardActions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Native list transport; shared feature callbacks remain the only mutation and navigation owners. */
class IosCommunityFeed internal constructor(
    initial: CommunityFeedPresentation, callbacks: CommunityFeedActions,
    private val media: IosCatalogueMedia, private val accounts: SessionReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val generation = accounts.accountGeneration()
    private val account = accounts.peekSession()?.userId
    private val signedIn = accounts.isActiveSession(accounts.peekSession())
    private val cards = linkedMapOf<String, IosCommunityCard>()
    private var actions: CommunityFeedActions? = callbacks
    private var closed = false
    private val state = MutableStateFlow(IosCommunityFeedState(null, emptyList(), 0))
    internal val visibleFractions = MutableStateFlow<Map<String, Double>>(emptyMap())

    init {
        update(initial, callbacks)
        scope.launch {
            accounts.observeSession().catch {
                if (it is CancellationException) throw it
                revoke()
            }.collect { if (!isCurrent()) revoke() }
        }
    }

    fun current() = state.value
    fun observe(onState: (IosCommunityFeedState) -> Unit) =
        IosCommunityFeedObservation(scope.launch { state.collect(onState) })

    fun perform(action: String) {
        if (isCurrent() && state.value.interactive) actions?.actions?.get(action)?.invoke()
    }

    fun visible(key: String, fraction: Double) {
        if (!isCurrent() || !state.value.interactive || !fraction.isFinite()) return
        if (key != "feed" && key !in cards) return
        visibleFractions.value = visibleFractions.value.toMutableMap().apply {
            if (fraction <= 0) remove(key) else put(key, fraction.coerceIn(0.0, 1.0))
        }
    }

    fun refresh(onComplete: () -> Unit): IosCommunityFeedObservation = IosCommunityFeedObservation(scope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            if (!isCurrent() || !state.value.interactive || actions?.actions?.containsKey("refresh") != true) return@launch
            val before = state.value.sessionGeneration
            perform("refresh")
            withTimeoutOrNull(30_000) {
                state.first { it.sessionGeneration != before || !it.active || actions?.actions?.containsKey("refresh") != true }
                state.first { !it.refreshing || !it.active }
            }
        } finally { onComplete() }
    })

    internal fun update(next: CommunityFeedPresentation, callbacks: CommunityFeedActions) {
        if (!isCurrent()) return
        actions = callbacks
        val keys = next.cards.map { it.key }.toSet()
        (cards.keys - keys).forEach { cards.remove(it)?.close() }
        next.cards.forEach { card ->
            val commands = callbacks.cards[card.key] ?: CommunityCardActions(emptyMap(), {})
            cards[card.key]?.update(card, commands)
                ?: run { cards[card.key] = IosCommunityCard(card, commands, media, accounts) }
        }
        if (!next.interactive) visibleFractions.value = emptyMap()
        else visibleFractions.value = visibleFractions.value.filterKeys { it == "feed" || it in keys }
        state.value = IosCommunityFeedState(next, next.cards.mapNotNull { cards[it.key] }, state.value.revision + 1)
    }

    private fun revoke() {
        closed = true
        actions = null
        cards.values.forEach { it.close() }
        cards.clear()
        visibleFractions.value = emptyMap()
        state.value = IosCommunityFeedState(null, emptyList(), state.value.revision + 1)
    }

    internal fun close() {
        revoke()
        scope.cancel()
    }

    private fun isCurrent() = !closed && accounts.accountGeneration() == generation &&
        accounts.peekSession()?.userId == account && accounts.isActiveSession(accounts.peekSession()) == signedIn
}

class IosCommunityFeedObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
