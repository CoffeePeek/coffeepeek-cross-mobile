package com.coffeepeek.data.repository

import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.core.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Compatibility adapter for the existing shop presentation contract. */
class FavoriteRepositoryImpl(
    private val favorites: FavoritesRepository,
    private val shops: ShopApiService,
    private val fileUrls: FileUrlResolver,
    private val sessions: SessionStore,
) : FavoriteRepository {
    override suspend fun getFavoriteIds(): Set<String> = favorites.getFavorites().getOrDefault(emptyList())
        .filter { it.kind == FavoriteKind.CoffeeShop }.mapNotNull { it.address?.slug }.toSet()

    override suspend fun isFavorite(shopId: String): Boolean = shopId in getFavoriteIds()

    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = try {
        val session = sessions.peekSession() ?: sessions.getSession()
        val account = session?.let { it.userId ?: it.accessToken }
        val entries = favorites.getFavorites(refresh = true).getOrThrow().filter { it.kind == FavoriteKind.CoffeeShop }
        val requests = Semaphore(4)
        val details = coroutineScope {
            entries.map { entry -> async {
                val slug = entry.address?.slug
                val loaded = slug?.let { requests.withPermit { shops.getShopDetails(it).getOrNull() } }
                currentCoroutineContext().ensureActive()
                val details = loaded?.toDomain(fileUrls) ?: CoffeeShopDetails(
                    CoffeeShop(slug ?: entry.id, entry.name.ifBlank { slug ?: "Кофейня" }, null,
                        cityName = null, priceRange = null, photoUrl = entry.coverPhotoUrl),
                )
                details.copy(shop = details.shop.copy(
                    id = slug ?: entry.id, publicAddress = entry.address, isFavorite = true,
                    title = entry.name.ifBlank { details.shop.title },
                    photoUrl = entry.coverPhotoUrl ?: details.shop.photoUrl,
                ))
            } }.awaitAll()
        }
        if (sessions.peekSession()?.let { it.userId ?: it.accessToken } != account) {
            throw CancellationException("Favorites account changed")
        }
        Result.success(details)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> {
        val publicAddress = shop.publicAddress
            ?: return Result.failure(IllegalArgumentException("У кофейни нет публичного адреса"))
        return favorites.setFavorite(FavoriteEntry(FavoriteKind.CoffeeShop, publicAddress,
            name = shop.title, coverPhotoUrl = shop.photoUrl), true)
    }

    override suspend fun removeFavorite(shopId: String): Result<Unit> {
        val entries = favorites.getFavorites().getOrElse { return Result.failure(it) }
        val entry = entries.firstOrNull { it.kind == FavoriteKind.CoffeeShop && (it.address?.slug == shopId || it.id == shopId) }
            ?: return Result.success(Unit)
        return favorites.setFavorite(entry, false)
    }

    override suspend fun clearAll() { favorites.clearCache() }
}
