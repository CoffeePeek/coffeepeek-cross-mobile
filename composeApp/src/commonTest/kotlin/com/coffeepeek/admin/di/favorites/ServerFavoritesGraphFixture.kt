package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.data.ServerRoasterFavorites
import com.coffeepeek.admin.session.ReadOnlySessions
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.Session
import com.coffeepeek.domain.feature.favorites.api.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
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
        single<FavoritesRepository> { server }
        single<RoasterRepository> { catalogs }
        single<PublicRoasterDetails> { FavoriteGraphPublicRoasters(catalogs) }
        single<ShopRepository> { FavoriteGraphShops(catalogs) }
        single<RoasterFavorites> { ServerRoasterFavorites(server, catalogs, sessions) }
    }

    fun close() = graph.close()
}

internal class FavoriteGraphServer : FavoritesRepository {
    private val address = PublicAddress("canonical", "/coffee-shop/canonical", 1, false)
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

    override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean) =
        setFavorite(FavoriteEntry(kind, address), favorite)

    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
        writes += Triple(entry.kind, entry.address?.slug ?: entry.drinkSlug, favorite)
        if (!favorite) state.value = state.value.map { rows -> rows.filterNot { it.id == entry.id } }
        return Result.success(Unit)
    }

    override fun clearCache() { clears++ }
}

internal class FavoriteGraphCatalogs : RoasterRepository {
    var reads = 0
    override suspend fun getRoasters() = Result.success(emptyList<RoasterSummary>()).also { reads++ }
    override suspend fun getRoaster(id: String): Result<RoasterDetails> {
        reads++
        return Result.failure(IllegalStateException("No catalog metadata"))
    }
    override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("No submission")
    override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> =
        error("No submission reads")
}

private class FavoriteGraphPublicRoasters(private val catalogs: FavoriteGraphCatalogs) : PublicRoasterDetails {
    override suspend fun summaries(): Result<List<PublicRoasterSummary>> = error("No catalog")
    override suspend fun load(slug: String): Result<PublicRoasterDescription> {
        catalogs.reads++
        return Result.failure(IllegalStateException("No catalog metadata"))
    }
}

private class FavoriteGraphShops(private val catalogs: FavoriteGraphCatalogs) : ShopRepository {
    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> {
        catalogs.reads++
        return Result.failure(IllegalStateException("No catalog metadata"))
    }
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("No search")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("No map")
    override suspend fun getCatalogs(): Result<ShopCatalogs> = error("No catalogs")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("No drink catalog")
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("No menu")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("No create")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> =
        error("No submission reads")
}
