package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.shop.api.PublicShopDetails
import com.coffeepeek.feature.shop.api.PublicShopSummary
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.roaster.api.PublicRoasterDescription
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterSummary
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository as ShopsPort
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class ServerFavoritesGraphFixture {
    val sessions = ReadOnlySessions(Session("test-token", userId = "A"))
    val server = FavoriteGraphServer()
    val catalogs = FavoriteGraphCatalogs()
    val graph = koinApplication { modules(serverFavoritesModule(), dependencies()) }
    val entry get() = graph.koin.get<FavoritesEntry>()
    val shops get() = graph.koin.get<ShopsPort>()
    val roasters get() = graph.koin.get<FavoriteRoastersRepository>()
    val drinks get() = graph.koin.get<FavoriteDrinksRepository>()

    private fun dependencies() = module {
        single<SessionReader> { sessions }
        single<AccountFavorites> { server }
        single<PublicRoasterDetails> { FavoriteGraphPublicRoasters(catalogs) }
        single<PublicShopDetails> { FavoriteGraphPublicShops(catalogs) }
        single<RoasterFavorites> { ServerRoasterFavorites(server, FavoriteGraphPublicRoasters(catalogs), sessions) }
    }

    fun close() = graph.close()
}

internal class FavoriteGraphServer : AccountFavorites {
    private val address = FavoriteAddress("canonical", "/coffee-shop/canonical", 1, false)
    val state = MutableStateFlow(Result.success(listOf(
        FavoriteEntry(FavoriteKind.CoffeeShop, address, name = "Shop"),
        FavoriteEntry(FavoriteKind.Roaster, address.copy(slug = "roast", canonicalPath = "/roasters/roast"),
            name = "Roaster", coverPhotoUrl = "https://fixture.invalid/logo"),
        FavoriteEntry(FavoriteKind.Drink, drinkSlug = "latte", name = "Latte"),
    )))
    var reads = 0
    var observations = 0
    var clears = 0
    var afterRead: () -> Unit = {}
    val writes = mutableListOf<Triple<FavoriteKind, String?, Boolean>>()

    override fun observeFavorites() = state.also { observations++ }
    override suspend fun getFavorites(refresh: Boolean) = state.value.also {
        reads++
        afterRead()
    }

    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
        writes += Triple(entry.kind, entry.address?.slug ?: entry.drinkSlug, favorite)
        if (!favorite) state.value = state.value.map { rows -> rows.filterNot { it.id == entry.id } }
        return Result.success(Unit)
    }

    override fun clearCache() { clears++ }
}

/** Counts the catalog reads the favorites graph makes; nothing here serves data. */
internal class FavoriteGraphCatalogs {
    var reads = 0
}

internal class FavoriteGraphPublicRoasters(private val catalogs: FavoriteGraphCatalogs) : PublicRoasterDetails {
    override suspend fun summaries(): Result<List<PublicRoasterSummary>> {
        catalogs.reads++
        return Result.success(emptyList())
    }
    override suspend fun load(slug: String): Result<PublicRoasterDescription> {
        catalogs.reads++
        return Result.failure(IllegalStateException("No catalog metadata"))
    }
}

private class FavoriteGraphPublicShops(private val catalogs: FavoriteGraphCatalogs) : PublicShopDetails {
    override suspend fun load(slug: String): Result<PublicShopSummary> {
        catalogs.reads++
        return Result.failure(IllegalStateException("No catalog metadata"))
    }
}
