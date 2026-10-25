package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.di.favorites.ResumedAfterAuth
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * What a guest asked for with the Shop heart. Signing in resets navigation, which destroys the shop screen, so the
 * intent lives here: one short-lived entry, in memory only, applied once for the next signed-in session.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class ShopFavoriteIntent(
    private val sessions: SessionReader,
    private val details: ShopDetailsRepository,
    private val favorites: FavoritesRepository,
    scope: CoroutineScope,
    private val onApplied: suspend (shopId: String, favorite: Boolean) -> Unit = { _, _ -> },
) {
    private class Intent(val shopId: String, val favorite: Boolean, val markedAt: TimeMark)

    private val pending = AtomicReference<Intent?>(null)

    init {
        scope.launch {
            sessions.observeSession().catch { if (it is CancellationException) throw it }.collect { session ->
                if (sessions.isActiveSession(session)) take()?.let { intent ->
                    val lease = ShopBrowsingSession(sessions, session)
                    launch { apply(intent, lease) }
                }
            }
        }
    }

    fun remember(shopId: String, favorite: Boolean) {
        pending.store(Intent(shopId, favorite, TimeSource.Monotonic.markNow()))
    }

    private fun take(): Intent? = pending.exchange(null)
        ?.takeIf { it.markedAt.elapsedNow() < TTL }

    private suspend fun apply(intent: Intent, lease: ShopBrowsingSession) {
        try {
            val result = write(intent, lease)
            if (result.isSuccess && lease.isCurrent()) onApplied(intent.shopId, intent.favorite)
        } finally {
            lease.revoke()
        }
    }

    private suspend fun write(intent: Intent, lease: ShopBrowsingSession): Result<Unit> = try {
        withContext(ResumedAfterAuth) {
            val guarded = lease.favorites(favorites)
            if (intent.favorite) {
                lease.details(details).getDetails(intent.shopId)
                    .fold({ guarded.save(it.toFavoriteSnapshot()) }, { Result.failure(it) })
            } else guarded.remove(intent.shopId)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object { val TTL = 10.minutes }
}

internal fun shopFavoriteIntent(
    sessions: SessionReader, details: ShopDetailsRepository, favorites: FavoritesRepository,
): ShopFavoriteIntent = ShopFavoriteIntent(sessions, details, favorites,
    CoroutineScope(SupervisorJob() + Dispatchers.Default),
    onApplied = { shopId, favorite -> FavoriteSync.notifyChanged(shopId, favorite) })
