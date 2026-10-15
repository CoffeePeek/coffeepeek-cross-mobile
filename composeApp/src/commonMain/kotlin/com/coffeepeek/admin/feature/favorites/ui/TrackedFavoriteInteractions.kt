package com.coffeepeek.admin.feature.favorites.ui

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.catch
import kotlin.time.TimeSource

internal class TrackedFavoriteInteractions(private val sessions: SessionRepository, private val scope: CoroutineScope) : FavoriteInteractions {
    private val lock = Mutex()
    private val pending = mutableSetOf<String>()
    private val mutations = mutableMapOf<String, Pair<String?, Job>>()
    private var deferred: (suspend () -> Unit)? = null
    private var deferredAt = TimeSource.Monotonic.markNow()
    init {
        scope.launch {
            sessions.observeSession().catch { Analytics.service?.syncIdentityNow(null, authRestoring = true) }.collect { session ->
                val account = accountKey()
                val obsolete = lock.withLock { mutations.values.filter { it.first != account }.map { it.second } }
                obsolete.forEach { it.cancel() }
                if (sessions.isActiveSession(session)) {
                    val action = lock.withLock { deferred.also { deferred = null } }
                    if (deferredAt.elapsedNow().inWholeMinutes < 10) action?.invoke()
                    else lock.withLock { pending.clear() }
                }
            }
        }
    }
    private fun accountKey(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
    override fun change(key: String, roaster: Boolean, desired: Boolean, action: suspend () -> Result<Unit>, completed: suspend (Result<Unit>) -> Unit) {
        scope.launch {
            val pendingKey = "${if (roaster) "roaster" else "shop"}:$key"
            if (!lock.withLock { pending.add(pendingKey) }) return@launch
            val entity = if (roaster) "roaster" else "coffee_shop"
            Analytics.track(AnalyticsEvent.FAVORITE_ATTEMPTED, "entity_type" to entity, "is_favorite" to desired)
            suspend fun perform(resumed: Boolean) {
                try {
                    val user = sessions.peekSession()?.userId
                    val account = accountKey()
                    val service = Analytics.service
                    if (user != null) service?.syncIdentityNow(user)
                    val operation = service?.operationScope()
                    val started = TimeSource.Monotonic.markNow()
                    val mutation = scope.async(start = CoroutineStart.LAZY) {
                        try { action() } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
                    }
                    lock.withLock { mutations[pendingKey] = account to mutation }
                    if (accountKey() != account) { mutation.cancel(); return }
                    mutation.start()
                    val result = mutation.await()
                    if (accountKey() != account || !sessions.isActiveSession(sessions.peekSession())) return
                    val duration = started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
                    if (operation != null) result.onSuccess {
                        service.track(AnalyticsEvent.FAVORITE_CHANGED, mapOf("entity_type" to entity, "is_favorite" to desired, "duration_ms" to duration, "resumed_after_auth" to resumed, "favorite_storage" to "server"), operation)
                    }.onFailure {
                        val error = safeAnalyticsError(it)
                        if (error.category != "cancelled") service.track(AnalyticsEvent.FAVORITE_FAILED, mapOf("entity_type" to entity, "duration_ms" to duration, "favorite_storage" to "server") + error.properties(), operation)
                    }
                    completed(result)
                } finally { lock.withLock { pending.remove(pendingKey); mutations.remove(pendingKey) } }
            }
            if (!runCatching { sessions.isLoggedIn() }.getOrDefault(false)) {
                lock.withLock {
                    // One short-lived intent, attached to this login. No action/account data on disk.
                    pending.clear()
                    pending.add(pendingKey)
                    deferredAt = TimeSource.Monotonic.markNow()
                    deferred = { scope.launch { perform(true) }; Unit }
                }
                if (sessions.isActiveSession(sessions.peekSession())) {
                    val resumed = lock.withLock { deferred.also { deferred = null } }
                    resumed?.invoke()
                    return@launch
                }
                Analytics.gate("favorites")
                Navigator.navigate(Navigator.Screen.Auth)
            } else perform(false)
        }
    }
}
