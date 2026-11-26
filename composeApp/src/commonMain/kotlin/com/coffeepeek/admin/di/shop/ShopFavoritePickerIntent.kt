package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.di.collections.CollectionPickerRequests
import com.coffeepeek.admin.di.collections.CollectionPickerLock
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * What a guest asked for with the Shop heart. Signing in resets navigation; reopen the picker without writing a favorite.
 */
internal class ShopFavoritePickerIntent(
    private val sessions: SessionReader,
    private val requests: CollectionPickerRequests,
    scope: CoroutineScope,
) : ShopFavoriteIntent {
    private class Intent(val shopId: String, val markedAt: TimeMark)

    private val lock = CollectionPickerLock()
    private var pending: Intent? = null

    init {
        scope.launch {
            sessions.observeSession().catch { if (it is CancellationException) throw it }.collect { session ->
                if (sessions.isActiveSession(session)) take()?.let { requests.request(CollectionTarget(CollectionTargetKind.CoffeeShop, it.shopId)) }
            }
        }
    }

    override fun remember(shopId: String, favorite: Boolean) = lock.withLock {
        pending = Intent(shopId, TimeSource.Monotonic.markNow())
    }

    private fun take(): Intent? = lock.withLock { pending.also { pending = null } }
        ?.takeIf { it.markedAt.elapsedNow() < TTL }

    private companion object { val TTL = 10.minutes }
}

internal fun shopFavoritePickerIntent(
    sessions: SessionReader, requests: CollectionPickerRequests,
): ShopFavoriteIntent = ShopFavoritePickerIntent(sessions, requests, CoroutineScope(SupervisorJob() + Dispatchers.Default))
