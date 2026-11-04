package com.coffeepeek.admin.feature.favorites.data

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.di.favorites.favoritesAccountId
import com.coffeepeek.admin.di.favorites.observeLoadedServerFavorites
import com.coffeepeek.admin.di.favorites.requireFavoritesAccount
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class ServerRoasterFavorites(
    private val favorites: FavoritesRepository,
    private val roasters: RoasterRepository,
    private val sessions: com.coffeepeek.core.session.SessionReader? = null,
) : RoasterFavorites {
    private val errors = MutableStateFlow<String?>(null)
    override fun observeErrors(): Flow<String?> = errors.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<List<CatalogItem>> = if (sessions == null) rows(null) else channelFlow {
        var observation: Job? = null
        var firstOwner = true
        try {
            sessions.observeSession().map { sessions.favoritesAccountId() }.distinctUntilChanged().collect { account ->
                observation?.cancel()
                errors.value = null
                if (!firstOwner || account == null) send(emptyList())
                firstOwner = false
                if (account != null) observation = launch {
                    rows(account).collect { items ->
                        currentCoroutineContext().ensureActive()
                        requireFavoritesAccount(sessions, account)
                        send(items)
                    }
                }
            }
        } finally { observation?.cancel() }
    }.buffer(Channel.RENDEZVOUS)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun rows(account: String?): Flow<List<CatalogItem>> = observeLoadedServerFavorites(favorites, sessions, account).mapLatest { result ->
        errors.value = if (result.isFailure) "Не удалось загрузить избранных обжарщиков" else null
        val entries = result.getOrDefault(emptyList()).filter { it.kind == FavoriteKind.Roaster }
        if (entries.isEmpty()) return@mapLatest emptyList()
        mappedRoasters(entries, account)
    }

    private suspend fun mappedRoasters(entries: List<FavoriteEntry>, account: String?): List<CatalogItem> {
        val response = roasters.getRoasters()
        response.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
        val catalog = response.getOrDefault(emptyList()).mapNotNull { roaster ->
            roaster.publicAddress?.slug?.let { it to roaster.toCatalogItem() }
        }.toMap()
        currentCoroutineContext().ensureActive()
        requireFavoritesAccount(sessions, account)
        val requests = Semaphore(4)
        return coroutineScope {
            entries.map { entry -> async { mappedRoaster(entry, catalog, requests, account) } }.awaitAll()
        }.also { requireFavoritesAccount(sessions, account) }
    }

    private suspend fun mappedRoaster(entry: FavoriteEntry, catalog: Map<String, CatalogItem>, requests: Semaphore,
        account: String?): CatalogItem {
        val slug = entry.address?.slug
        slug?.let(catalog::get)?.let { item -> return item.copy(address = entry.address,
            name = entry.name.ifBlank { item.name }, photoUrl = entry.coverPhotoUrl ?: item.photoUrl) }
        val result = slug?.let { requests.withPermit { roasters.getRoaster(it) } }
        result?.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
        currentCoroutineContext().ensureActive()
        requireFavoritesAccount(sessions, account)
        val details = result?.getOrNull()
        return CatalogItem(
            id = slug ?: entry.id, slug = slug.orEmpty(), name = entry.name.ifBlank { details?.name ?: slug ?: "Обжарщик" },
            photoUrl = entry.coverPhotoUrl ?: details?.photos?.firstOrNull()?.fullUrl,
            address = entry.address, description = details?.about,
        )
    }

    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
        val address = roaster.address
            ?: return Result.failure(IllegalArgumentException("У обжарщика нет публичного адреса"))
        return favorites.setFavorite(FavoriteEntry(FavoriteKind.Roaster, address,
            name = roaster.name, coverPhotoUrl = roaster.photoUrl), isFavorite)
    }

    override suspend fun removeForAccount(roaster: CatalogItem, expectedUserId: String): Result<Unit> {
        val ownerSessions = sessions ?: return Result.failure(IllegalStateException("Account guard is unavailable"))
        val session = ownerSessions.peekSession()
        if (session?.userId != expectedUserId || !ownerSessions.isActiveSession(session))
            throw kotlinx.coroutines.CancellationException("Favorites account changed")
        val result = setFavorite(roaster, false)
        currentCoroutineContext().ensureActive()
        if (ownerSessions.peekSession()?.userId != expectedUserId)
            throw kotlinx.coroutines.CancellationException("Favorites account changed")
        return result
    }
}
