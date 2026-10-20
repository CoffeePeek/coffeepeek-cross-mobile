package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * What a guest asked for with the Shop heart. Signing in resets navigation, which destroys the shop screen, so the
 * intent lives here: one short-lived entry, in memory only, applied once for the next signed-in session.
 */
internal class ShopFavoriteIntent(
    private val sessions: SessionRepository,
    private val details: ShopDetailsRepository,
    private val favorites: FavoritesRepository,
    scope: CoroutineScope,
    private val onApplied: suspend (shopId: String, favorite: Boolean) -> Unit = { _, _ -> },
) {
    private class Intent(val shopId: String, val favorite: Boolean, val markedAt: TimeMark)

    private val lock = Any()
    private var pending: Intent? = null

    init {
        scope.launch {
            sessions.observeSession().catch { }.collect { session ->
                if (sessions.isActiveSession(session)) take()?.let { apply(it) }
            }
        }
    }

    fun remember(shopId: String, favorite: Boolean) = synchronized(lock) {
        pending = Intent(shopId, favorite, TimeSource.Monotonic.markNow())
    }

    private fun take(): Intent? = synchronized(lock) { pending.also { pending = null } }
        ?.takeIf { it.markedAt.elapsedNow() < TTL }

    private suspend fun apply(intent: Intent) {
        val result = try {
            if (intent.favorite) {
                details.getDetails(intent.shopId).fold({ favorites.save(it.toFavoriteSnapshot()) }, { Result.failure(it) })
            } else favorites.remove(intent.shopId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        if (result.isSuccess) onApplied(intent.shopId, intent.favorite)
    }

    private companion object { val TTL = 10.minutes }
}

internal fun shopFavoriteIntent(
    sessions: SessionRepository, details: ShopDetailsRepository, favorites: FavoritesRepository,
): ShopFavoriteIntent = ShopFavoriteIntent(sessions, details, favorites,
    CoroutineScope(SupervisorJob() + Dispatchers.Default),
    onApplied = { shopId, favorite -> FavoriteSync.notifyChanged(shopId, favorite) })
