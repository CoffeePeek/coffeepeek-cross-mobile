package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.feature.community.ui.FeedSessions
import com.coffeepeek.admin.di.favorites.serverFavoriteShopsModule
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository as ServerFavorites
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Shared port contracts, with no platform location or real server calls. */
class DiscoveryAdaptersTest {
    @Test fun theSharedGraphReadsMembershipFromTheExistingServerWithoutHydratingCards() = runTest {
        val graph = koinApplication {
            modules(module {
                single<SessionReader> { FeedSessions(Session("token", userId = "user")) }
                single<ServerFavorites> { Server() }
                single<ShopRepository> { Shops(Result.failure(IllegalStateException("unused"))) }
            }, serverFavoriteShopsModule())
        }
        try {
            assertEquals(setOf("shop"), AppDiscoveryFavorites(graph.koin.get()).favoriteIds())
        } finally { graph.close() }
    }

    @Test fun theAccountKeyIsTheUserIdElseTheTokenAndNullForAGuestOrAnInactiveSession() = runTest {
        val sessions = FeedSessions(null)
        val account = SessionDiscoveryAccount(sessions)
        assertNull(account.currentAccountKey())
        sessions.applySession(Session("token", userId = "user-1"))
        assertEquals("user-1", account.accountKey.first())
        sessions.applySession(Session("token-only"))
        assertEquals("token-only", account.currentAccountKey())
        sessions.applySession(Session(" ", userId = "user-1"))
        assertNull(account.currentAccountKey())
    }

    @Test fun theFavoriteSnapshotKeepsWhatTheCardShowsAndTheFavoritesOwnerGetsTheChangeAndTheOtherScreensAreTold() = runTest(UnconfinedTestDispatcher()) {
        val repository = Favorites()
        val adapter = AppDiscoveryFavorites(repository)
        val changes = async(start = CoroutineStart.UNDISPATCHED) { FavoriteSync.changes.first() }
        val shop = ShopSummary("slug", "Shop", rating = 4.5, checkInCount = 3, address = "Street", priceLevel = PriceLevel.Moderate, isOpen = true,
            tags = listOf("Wi-Fi"), brewMethods = listOf("V60"), roasterLogos = listOf("logo"), coordinates = Coordinates(53.9, 27.5), photoUrl = "photo")
        assertTrue(adapter.setFavorite(shop, true).isSuccess)
        val saved = repository.saved.single()
        assertEquals(FavoriteShop("slug", "Shop", 4.5, 3, null, "$$", "photo", "Street", 53.9, 27.5, true, listOf("Wi-Fi"), listOf("V60"), listOf("logo"), "slug"), saved)
        assertEquals(FavoriteSync.FavoriteChange("slug", true), changes.await())
        assertTrue(adapter.setFavorite(shop, false).isSuccess)
        assertEquals(listOf("slug"), repository.removed)
    }

    @Test fun aFailedChangeIsReportedAndNotAnnounced() = runTest(UnconfinedTestDispatcher()) {
        val repository = Favorites(failure = IllegalStateException("offline"))
        val adapter = AppDiscoveryFavorites(repository)
        val changes = async(start = CoroutineStart.UNDISPATCHED) { FavoriteSync.changes.first() }
        assertTrue(adapter.setFavorite(ShopSummary("a", "A"), true).isFailure)
        assertTrue(changes.isActive)
        changes.cancel()
    }

    @Test fun theFavoriteIdsSkipAFailingAnswerAndStartFromTheFirstOne() = runTest {
        val repository = Favorites(ids = flow {
            emit(Result.success(setOf("a")))
            emit(Result.failure(IllegalStateException("offline")))
            emit(Result.success(setOf("a", "b")))
        })
        val adapter = AppDiscoveryFavorites(repository)
        assertEquals(listOf(setOf("a"), setOf("a", "b")), adapter.ids.toList())
        assertEquals(setOf("a"), adapter.favoriteIds())
    }

    @Test fun theRoasterListOfTheFiltersIsTheLegacyCatalog() = runTest {
        val shops = Shops(Result.success(ShopCatalogs(roasters = listOf(CatalogItem("r", "Roaster", photoUrl = "logo")))))
        assertEquals(listOf(CatalogEntry("r", "Roaster", "logo")), roasterEntries(shops)().getOrThrow())
        assertTrue(roasterEntries(Shops(Result.failure(IllegalStateException("offline"))))().isFailure)
    }

    private class Server : ServerFavorites {
        override fun observeFavorites() = flow {
            emit(Result.success(listOf(FavoriteEntry(FavoriteKind.CoffeeShop, PublicAddress("shop", "/coffee-shop/shop", 1, false)))))
        }
        override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> = error("unused")
        override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit> = error("unused")
        override fun clearCache() = error("unused")
    }

    private class Favorites(private val failure: Throwable? = null, private val ids: kotlinx.coroutines.flow.Flow<Result<Set<String>>> = flow { emit(Result.success(emptySet())) }) : FavoritesRepository {
        val saved = mutableListOf<FavoriteShop>()
        val removed = mutableListOf<String>()
        override fun observe() = flow<Result<List<FavoriteShop>>> { emit(Result.success(emptyList())) }
        override fun observeIds() = ids
        override suspend fun read(): Result<List<FavoriteShop>> = Result.success(emptyList())
        override suspend fun save(shop: FavoriteShop): Result<Unit> = failure?.let { Result.failure(it) } ?: Result.success(Unit).also { saved += shop }
        override suspend fun remove(shopId: String): Result<Unit> = failure?.let { Result.failure(it) } ?: Result.success(Unit).also { removed += shopId }
        override suspend fun clear(): Result<Unit> = Result.success(Unit)
    }

    private class Shops(private val catalogs: Result<ShopCatalogs>) : ShopRepository {
        override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("unused")
        override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("unused")
        override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
        override suspend fun getCatalogs() = catalogs
        override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("unused")
        override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("unused")
        override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
        override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("unused")
    }
}
