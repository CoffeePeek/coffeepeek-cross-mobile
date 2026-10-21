package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.analytics.Entity

import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository as ServerFavorites
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Application-owned adaptation; server membership is authoritative, never the old Room snapshots. */
internal class ServerFavoriteShopsRepository(
    private val server: ServerFavorites,
    private val sessions: SessionRepository,
    private val detail: suspend (String) -> Result<CoffeeShopDetails>,
) : FavoritesRepository {
    override fun observeIds(): Flow<Result<Set<String>>> = server.observeFavorites().map { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (owner() == null) Result.success(emptySet()) else result.map { entries ->
            entries.filter { it.kind == FavoriteKind.CoffeeShop }.mapNotNull { it.address?.slug }.toSet()
        }
    }

    override fun observe(): Flow<Result<List<FavoriteShop>>> = channelFlow {
        var observation: Job? = null
        try {
            sessions.observeSession().map { owner() }.distinctUntilChanged().collect { account ->
                observation?.cancel()
                send(Result.success(emptyList()))
                if (account != null) observation = launch {
                    server.observeFavorites().collectLatest { result ->
                        val mapped = resultOf { mapRows(result.getOrThrow(), account) }
                        ensureActive()
                        requireOwner(account)
                        send(mapped)
                    }
                }
            }
        } finally { observation?.cancel() }
    }.buffer(Channel.RENDEZVOUS)

    override suspend fun read(): Result<List<FavoriteShop>> = resultOf {
        val account = owner() ?: return@resultOf emptyList()
        mapRows(server.getFavorites(refresh = true).getOrThrow(), account)
    }

    override suspend fun save(shop: FavoriteShop): Result<Unit> =
        trackFavoriteMutation(Entity.Shop, true, owner()) { saveUntracked(shop) }

    private suspend fun saveUntracked(shop: FavoriteShop): Result<Unit> = resultOf {
        val account = requireNotNull(owner()) { "Sign in to save favorites" }
        val resolved = detail(shop.id).getOrThrow().shop
        requireOwner(account)
        val address = requireNotNull(resolved.publicAddress) { "Shop has no public address" }
        server.setFavorite(FavoriteEntry(FavoriteKind.CoffeeShop, address,
            name = resolved.title, coverPhotoUrl = resolved.photoUrl), true).getOrThrow()
        requireOwner(account)
    }

    override suspend fun remove(shopId: String): Result<Unit> =
        trackFavoriteMutation(Entity.Shop, false, owner()) { removeUntracked(shopId) }

    private suspend fun removeUntracked(shopId: String): Result<Unit> = resultOf {
        val account = requireNotNull(owner()) { "Sign in to change favorites" }
        val entry = server.getFavorites().getOrThrow().firstOrNull {
            it.kind == FavoriteKind.CoffeeShop && (it.address?.slug == shopId || it.id == shopId)
        }
        requireOwner(account)
        if (entry != null) server.setFavorite(entry, false).getOrThrow()
        requireOwner(account)
    }

    /** Logout clears memory only, never server membership. */
    override suspend fun clear(): Result<Unit> = resultOf { server.clearCache() }

    private suspend fun mapRows(entries: List<FavoriteEntry>, account: String): List<FavoriteShop> = coroutineScope {
        val requests = Semaphore(4)
        entries.filter { it.kind == FavoriteKind.CoffeeShop }.map { entry -> async {
            requireOwner(account)
            val resolved = entry.address?.slug?.let { slug -> requests.withPermit { detail(slug) } }
            resolved?.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            ensureActive()
            requireOwner(account)
            val details = resolved?.getOrNull()
            val shop = details?.shop
            FavoriteShop(id = entry.address?.slug ?: entry.id, title = entry.name.ifBlank { shop?.title.orEmpty() },
                rating = shop?.rating, reviewCount = shop?.reviewCount ?: 0, cityName = shop?.cityName,
                priceRange = shop?.priceRange, photoUrl = entry.coverPhotoUrl ?: shop?.photoUrl,
                address = details?.location?.address ?: shop?.address,
                latitude = details?.location?.latitude, longitude = details?.location?.longitude,
                isOpen = shop?.isOpen ?: false, tags = shop?.tags.orEmpty(),
                brewMethods = shop?.brewMethods.orEmpty(), roasterPhotoUrls = shop?.roasterPhotoUrls.orEmpty(),
                publicSlug = entry.address?.slug)
        } }.awaitAll().also { requireOwner(account) }
    }

    private fun owner() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
    private fun requireOwner(account: String) {
        if (owner() != account) throw CancellationException("Favorites account changed")
    }
}

private suspend inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) { throw cancelled }
catch (error: Exception) { Result.failure(error) }
