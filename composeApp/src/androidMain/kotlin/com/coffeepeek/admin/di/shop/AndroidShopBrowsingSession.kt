package com.coffeepeek.admin.di.shop

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/** A permanently revocable account lease; token renewal for the same account is not replacement. */
internal class AndroidShopBrowsingSession(
    private val sessions: SessionReader,
    session: Session? = sessions.peekSession(),
    private val visibility: AuthorVisibility = AuthorVisibility.None,
) {
    private val visibilityRevision = visibility.visibility.value.revision
    private val loggedIn = sessions.isActiveSession(session)
    private val accountId = session?.userId.takeIf { loggedIn }
    @Volatile private var revoked = false

    fun isCurrent(session: Session? = sessions.peekSession()): Boolean = !revoked &&
        visibility.visibility.value.revision == visibilityRevision &&
        sessions.isActiveSession(session) == loggedIn && (!loggedIn || session?.userId == accountId)

    fun revoke() { revoked = true }

    fun details(repository: ShopDetailsRepository): ShopDetailsRepository = object : ShopDetailsRepository {
        override suspend fun getDetails(shopId: String) = withSession {
            if (!visibility.visibility.value.ready) {
                val refreshed = visibility.refresh()
                refreshed.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                ensureCurrent()
                refreshed.exceptionOrNull()?.let { return@withSession Result.failure(it) }
            }
            repository.getDetails(shopId).map { details ->
                val snapshot = visibility.visibility.value
                details.copy(checkIns = details.checkIns.filterNot { snapshot.hides(it.authorId, it.authorSlug) })
            }
        }
    }

    fun votes(repository: ShopCheckInVoteRepository): ShopCheckInVoteRepository = object : ShopCheckInVoteRepository {
        override suspend fun setHelpful(checkInId: String, helpful: Boolean) = withSession {
            if (!loggedIn) return@withSession Result.failure(IllegalStateException("Sign in before voting"))
            repository.setHelpful(checkInId, helpful)
        }
    }

    fun favorites(repository: FavoritesRepository): FavoritesRepository = object : FavoritesRepository {
        override fun observe() = channelFlow {
            withSession {
                if (loggedIn) repository.observe().collect { send(it) }
                else send(Result.success(emptyList<FavoriteShop>()))
            }
        }
        override fun observeIds() = channelFlow {
            withSession {
                if (loggedIn) repository.observeIds().collect { send(it) }
                else send(Result.success(emptySet<String>()))
            }
        }
        override suspend fun read() = withSession {
            if (loggedIn) repository.read() else Result.success(emptyList())
        }
        override suspend fun save(shop: FavoriteShop) = withSession {
            if (!loggedIn) return@withSession Result.failure(IllegalStateException("Sign in before saving favorites"))
            repository.save(shop)
        }
        override suspend fun remove(shopId: String) = withSession {
            if (!loggedIn) return@withSession Result.failure(IllegalStateException("Sign in before removing favorites"))
            repository.remove(shopId)
        }
        override suspend fun clear() = withSession {
            if (loggedIn) repository.clear() else Result.failure(IllegalStateException("Sign in before clearing favorites"))
        }
    }

    suspend fun viewer(users: UserRepository): Result<ShopViewer> = withSession {
        if (!loggedIn) return@withSession Result.success(ShopViewer(false, null))
        val profile = users.getMe()
        profile.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        // Preserve legacy fallback: personal check-in IDs can establish ownership if profile lookup fails.
        Result.success(ShopViewer(true, profile.getOrNull()?.address?.slug))
    }

    private suspend fun <T> withSession(block: suspend () -> T): T = coroutineScope {
        ensureCurrent()
        val operation = async(start = CoroutineStart.LAZY) {
            ensureCurrent()
            block().also { ensureCurrent() }
        }
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            combine(sessions.observeSession(), visibility.visibility) { session, _ -> session }.catch { error ->
                if (error is CancellationException) throw error
                revoke()
                operation.cancel(CancellationException("Could not observe Shop session", error))
            }.collect { session ->
                if (!isCurrent(session)) {
                    revoke()
                    operation.cancel(CancellationException("Shop session was replaced"))
                }
            }
        }
        try { operation.await() } finally { observer.cancel() }
    }

    private fun ensureCurrent() {
        if (!isCurrent()) {
            revoke()
            throw CancellationException("Shop session was replaced")
        }
    }
}
