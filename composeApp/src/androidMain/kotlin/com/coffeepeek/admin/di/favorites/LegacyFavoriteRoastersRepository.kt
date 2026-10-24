package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.analytics.Entity

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Temporary composition bridge: the existing store/JSON and catalogue remain authoritative. */
internal class LegacyFavoriteRoastersRepository(
    private val favorites: RoasterFavorites,
    private val sessions: SessionReader,
    private val loadPhoto: suspend (String) -> Result<String?>,
) : FavoriteRoastersRepository {
    private class Snapshot(val owner: String, val items: Map<String, CatalogItem>)
    private val snapshot = MutableStateFlow<Snapshot?>(null)
    private val photoRequests = Semaphore(4)

    override fun observe(): Flow<Result<List<FavoriteRoaster>>> = channelFlow {
        // A new owner must clear rows without joining non-cooperative old photo requests.
        val activeOwner = MutableStateFlow<Any?>(null)
        var ownerJob: Job? = null
        try {
            sessions.observeSession()
                .map { session -> session?.userId?.takeIf { sessions.isActiveSession(session) && it.isNotBlank() } }
                .distinctUntilChanged()
                .collect { owner ->
                    val lease = Any()
                    activeOwner.value = lease
                    ownerJob?.cancel()
                    snapshot.value = null
                    send(Result.success(emptyList()))
                    if (owner == null) return@collect
                    ownerJob = launch {
                        var photos: Job? = null
                        val currentRows = MutableStateFlow<Snapshot?>(null)
                        // Match the legacy screen's per-owner, per-observation detail cache.
                        val photoCache = MutableStateFlow<Map<String, String?>>(emptyMap())
                        val rowsLock = Mutex()
                        try {
                            favorites.observeFavorites().combine(favorites.observeErrors()) { items, error -> items to error }
                                .distinctUntilChanged().collect roasters@{ (items, error) ->
                                if (activeOwner.value !== lease) return@roasters
                                ensureOwner(owner)
                                if (error != null) {
                                    send(Result.failure(IllegalStateException(error)))
                                    return@roasters
                                }
                                photos?.cancel()
                                val visibleSlugs = items.mapNotNull { it.address?.slug }.toSet()
                                photoCache.value = photoCache.value.filterKeys { it in visibleSlugs }
                                val captured = Snapshot(owner, items.associateBy { it.roasterFavoriteId })
                                currentRows.value = captured
                                snapshot.value = captured
                                var rows = items.map { item -> item.toFavorite().let {
                                    it.copy(photoUrl = it.photoUrl ?: item.address?.slug?.let { slug -> photoCache.value[slug] })
                                } }
                                send(Result.success(rows))
                                photos = launch {
                                    items.filter { it.photoUrl.isNullOrBlank() }.forEach { item ->
                                        val slug = item.address?.slug?.takeIf(String::isNotBlank) ?: return@forEach
                                        if (photoCache.value.containsKey(slug)) return@forEach
                                        launch {
                                            val result = photoRequests.withPermit { loadPhoto(slug) }
                                            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                                            currentCoroutineContext().ensureActive()
                                            ensureOwner(owner)
                                            rowsLock.withLock {
                                                if (activeOwner.value === lease && currentRows.value === captured) {
                                                    if (result.isSuccess) photoCache.value = photoCache.value + (slug to result.getOrNull())
                                                    result.getOrNull()?.takeIf(String::isNotBlank)?.let { url ->
                                                        rows = rows.map {
                                                            if (it.id == item.roasterFavoriteId) it.copy(photoUrl = url) else it
                                                        }
                                                        send(Result.success(rows))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (activeOwner.value === lease) send(Result.failure(error))
                        } finally {
                            currentRows.value = null
                            photos?.cancel()
                        }
                    }
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            activeOwner.value = null
            snapshot.value = null
            ownerJob?.cancel()
            send(Result.success(emptyList()))
            send(Result.failure(error))
        } finally {
            activeOwner.value = null
            ownerJob?.cancel()
        }
    // Owner revocation must reach the consumer before a following retained-content error.
    // Conflation could drop the clearing emission while the UI collector is busy.
    }.buffer(Channel.RENDEZVOUS)

    override suspend fun remove(roasterId: String): Result<Unit> {
        val captured = snapshot.value ?: return Result.failure(IllegalStateException("Favorites not loaded"))
        val item = captured.items[roasterId] ?: return Result.failure(IllegalArgumentException("Unknown saved roaster"))
        ensureOwner(captured.owner)
        val result = trackFavoriteMutation(Entity.Roaster, false, captured.owner) {
            favorites.removeForAccount(item, captured.owner)
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result
    }

    private fun ensureOwner(owner: String) {
        val session = sessions.peekSession()
        if (!sessions.isActiveSession(session) || session?.userId != owner) {
            throw CancellationException("Favorites account changed")
        }
    }
}

private fun CatalogItem.toFavorite() = FavoriteRoaster(
    id = roasterFavoriteId, title = name, publicSlug = address?.slug?.takeIf(String::isNotBlank),
    photoUrl = photoUrl?.takeIf(String::isNotBlank),
    coffeeShopsCount = coffeeShopsCount, coffeeProductsCount = coffeeProductsCount,
    description = description?.takeIf(String::isNotBlank),
)
