package com.coffeepeek.admin.analytics

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

internal enum class AnalyticsConsent { Unknown, Accepted, Declined }
internal data class AnalyticsContext(val app: String, val version: String, val environment: String)
internal data class AnalyticsScope(val generation: Long, val page: String)
internal data class AnalyticsState(
    val consent: AnalyticsConsent = AnalyticsConsent.Unknown,
    val restored: Boolean = false,
    val consentPersisted: Boolean = false,
    val consentRevision: Long = 0,
    val authReady: Boolean = false,
    val userId: String? = null,
    val generation: Long = 0,
    val page: String = "other",
    val visit: String = "",
    val visitSequence: Long = 0,
    val foreground: Boolean = false,
    val ready: Boolean = false,
    val overlays: List<String> = emptyList(),
)

internal interface AnalyticsConsentStore {
    suspend fun read(): AnalyticsConsent
    suspend fun write(consent: AnalyticsConsent)
}

/** SDK calls belong to the adapter, never to feature UI or domain. */
internal interface AnalyticsTransport {
    fun start(allow: () -> Boolean, userId: String?)
    fun identify(userId: String)
    fun capture(event: String, properties: Map<String, Any>)
    fun screen(page: String, properties: Map<String, Any>)
    fun stopDelivery() {}
    /** Stop delivery, reset identity, close, then delete only this project's analytics queue. */
    fun erase(resetIdentity: Boolean = true)
}

internal class AnalyticsService(
    private val context: AnalyticsContext,
    private val store: AnalyticsConsentStore,
    private val transport: AnalyticsTransport,
    scope: CoroutineScope,
    private val enabled: Boolean,
) {
    private val mutableState = MutableStateFlow(AnalyticsState())
    val state = mutableState.asStateFlow()
    private val commands = Channel<Command>(Channel.UNLIMITED)
    // The only pre-SDK queue is memory-only, capped at 50. Controls cannot be dropped.
    private val pending = MutableStateFlow(0)
    private var active = false
    private var sdkUser: String? = null
    private var sdkGeneration = -1L
    private var sdkRevision = 0L
    private var lastScreen = ""
    private var queueAge = TimeSource.Monotonic.markNow()
    // Correlation only: never persisted or included in event properties.
    private data class PasswordSignUp(val email: String, val created: kotlin.time.TimeMark)
    private val passwordSignUp = MutableStateFlow<PasswordSignUp?>(null)

    init {
        scope.launch {
            for (command in commands) {
                try {
                    when (command) {
                        is Command.Reconcile -> reconcile()
                        is Command.Pause -> { if (active) erase(command.resetIdentity); reconcile() }
                        is Command.Expire -> if (active && queueAge.elapsedNow().inWholeMinutes >= 60) { erase(false); reconcile() }
                        is Command.Event -> {
                            if (command.generation == state.value.generation && allowed()) {
                                reconcile()
                                if (active) transport.capture(command.name, command.properties)
                            }
                        }
                        is Command.SaveConsent -> {
                            // Revocation is already effective in memory, even if persistence fails.
                            store.write(command.consent)
                            mutableState.update { if (it.consentRevision == command.revision) it.copy(consentPersisted = true) else it }
                            if (command.consent == AnalyticsConsent.Declined) erase()
                            reconcile()
                        }
                    }
                } catch (_: Exception) {
                    runCatching { erase() }
                    mutableState.update { it.copy(ready = false, consent = if (command is Command.SaveConsent) AnalyticsConsent.Declined else it.consent) }
                } finally {
                    if (command is Command.Event) pending.update { (it - 1).coerceAtLeast(0) }
                    command.done?.complete(Unit)
                }
            }
        }
        scope.launch {
            while (true) {
                delay(60_000)
                commands.send(Command.Expire)
            }
        }
    }

    suspend fun restoreConsent() {
        val consent = runCatching { store.read() }.getOrDefault(AnalyticsConsent.Unknown)
        mutableState.update { if (it.restored) it else it.copy(consent = consent, restored = true, consentPersisted = true) }
        reconcileAndWait()
    }

    suspend fun setConsent(consent: AnalyticsConsent) {
        if (consent == AnalyticsConsent.Unknown) return
        if (consent == AnalyticsConsent.Declined) passwordSignUp.value = null
        val snapshot = mutableState.updateAndGet { it.copy(consent = consent, restored = true, consentPersisted = false,
            consentRevision = it.consentRevision + 1, generation = it.generation + 1, ready = false) }
        if (consent == AnalyticsConsent.Declined) runCatching { transport.stopDelivery() }
        val done = CompletableDeferred<Unit>()
        commands.send(Command.SaveConsent(consent, snapshot.consentRevision, done))
        done.await()
    }

    suspend fun syncIdentity(userId: String?, authRestoring: Boolean = false) {
        syncIdentityNow(userId, authRestoring)
        reconcileAndWait()
    }

    fun syncIdentityNow(userId: String?, authRestoring: Boolean = false) {
        val previous = state.value.userId
        mutableState.update {
            val changed = it.userId != userId || it.authReady == authRestoring
            it.copy(userId = userId, authReady = !authRestoring,
                generation = if (changed) it.generation + 1 else it.generation,
                ready = if (changed) false else it.ready)
        }
        if (previous != null && previous != userId) {
            runCatching { transport.stopDelivery() }
            commands.trySend(Command.Pause(true))
        } else commands.trySend(Command.Reconcile())
    }

    fun setForeground(foreground: Boolean) {
        if (state.value.foreground == foreground) return
        mutableState.update { it.copy(foreground = foreground, ready = if (foreground) it.ready else false) }
        if (!foreground) {
            runCatching { transport.stopDelivery() }
            commands.trySend(Command.Pause(false))
        } else commands.trySend(Command.Reconcile())
    }

    fun overlay(id: String, visible: Boolean) {
        mutableState.update { it.copy(overlays = if (visible) (it.overlays - id) + id else it.overlays - id) }
    }

    fun screenShown(page: String, visit: String) {
        if (page !in pageTypes) return
        mutableState.update { it.copy(page = page, visit = visit, visitSequence = if (it.page == page && it.visit == visit) it.visitSequence else it.visitSequence + 1) }
        commands.trySend(Command.Reconcile())
    }

    fun operationScope(page: String = state.value.page) = AnalyticsScope(state.value.generation, page.takeIf { it in pageTypes } ?: "other")

    fun rememberPasswordSignUp(email: String) {
        passwordSignUp.value = if (enabled && state.value.consent == AnalyticsConsent.Accepted &&
            state.value.consentPersisted && email.isNotBlank()) {
            PasswordSignUp(email.trim().lowercase(), TimeSource.Monotonic.markNow())
        } else null
    }

    fun completePasswordSignUp(email: String?) {
        val pending = passwordSignUp.getAndUpdate { null } ?: return
        if (pending.email == email?.trim()?.lowercase() && pending.created.elapsedNow().inWholeHours < 1) {
            track(AnalyticsEvent.SIGN_UP_COMPLETED, mapOf("method" to "password"))
        }
    }

    fun track(event: AnalyticsEvent, properties: Map<String, Any>, operation: AnalyticsScope = operationScope()): Boolean {
        val current = state.value
        if (!allowed() || operation.generation != current.generation) return false
        val safe = AnalyticsSchema.validate(event, properties) ?: return false
        while (true) {
            val count = pending.value
            if (count >= 50) return false
            if (pending.compareAndSet(count, count + 1)) break
        }
        val accepted = commands.trySend(Command.Event(event.wire, safe + envelope(current, operation.page), current.generation)).isSuccess
        if (!accepted) pending.update { (it - 1).coerceAtLeast(0) }
        return accepted
    }

    private fun allowed() = enabled && state.value.let {
        it.restored && it.consentPersisted && it.consent == AnalyticsConsent.Accepted &&
            it.authReady && !it.userId.isNullOrBlank() && it.foreground
    }

    private fun envelope(current: AnalyticsState, page: String) = mapOf(
        "app" to context.app, "event_schema_version" to 1, "app_version" to context.version,
        "environment" to context.environment, "auth_state" to if (current.userId == null) "guest" else "signed_in",
        "page_type" to page,
    )

    private suspend fun reconcileAndWait() {
        val done = CompletableDeferred<Unit>()
        commands.send(Command.Reconcile(done))
        done.await()
    }

    private fun reconcile() {
        val current = state.value
        if (!allowed()) {
            if (active) erase(resetIdentity = current.consent != AnalyticsConsent.Accepted || !current.authReady)
            return
        }
        // Collection starts only after login; an identified account must never be linked to another.
        if (active && sdkUser != null && sdkUser != current.userId) erase()
        if (!active) {
            sdkGeneration = current.generation
            val revision = ++sdkRevision
            transport.start({ allowed() && revision == sdkRevision && sdkGeneration == state.value.generation }, current.userId)
            active = true
            queueAge = TimeSource.Monotonic.markNow()
            sdkUser = null
        }
        if (sdkUser != current.userId) {
            current.userId?.let(transport::identify)
            sdkUser = current.userId
        }
        sdkGeneration = current.generation
        mutableState.update { it.copy(ready = true) }
        val screenKey = "${current.generation}:${current.visit}:${current.visitSequence}:${current.page}"
        if (current.visit.isNotEmpty() && screenKey != lastScreen) {
            transport.screen(current.page, envelope(current, current.page))
            lastScreen = screenKey
        }
    }

    private fun erase(resetIdentity: Boolean = true) {
        sdkRevision++
        transport.erase(resetIdentity)
        active = false
        sdkUser = null
        sdkGeneration = -1
        mutableState.update { it.copy(ready = false) }
    }

    private sealed class Command(open val done: CompletableDeferred<Unit>? = null) {
        data object Expire : Command()
        data class Pause(val resetIdentity: Boolean) : Command()
        data class Reconcile(override val done: CompletableDeferred<Unit>? = null) : Command(done)
        data class SaveConsent(val consent: AnalyticsConsent, val revision: Long, override val done: CompletableDeferred<Unit>) : Command(done)
        data class Event(val name: String, val properties: Map<String, Any>, val generation: Long) : Command()
    }
}

/** Configured exactly once by the application composition root. Defaults to no collection. */
internal object Analytics {
    var service: AnalyticsService? = null
        private set
    fun install(service: AnalyticsService) { this.service = service }
    fun track(event: AnalyticsEvent, vararg properties: Pair<String, Any>): Boolean =
        service?.track(event, mapOf(*properties)) == true
    fun gate(feature: String) = track(AnalyticsEvent.AUTH_GATE_SHOWN, "feature" to feature)
    fun shopAction(action: String, entry: String = "shop_detail") =
        track(AnalyticsEvent.SHOP_ACTION_CLICKED, "action" to action, "entry_point" to entry)
}
