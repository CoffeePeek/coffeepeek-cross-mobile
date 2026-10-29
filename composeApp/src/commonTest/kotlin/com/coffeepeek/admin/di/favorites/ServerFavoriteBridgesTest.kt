package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.feature.favorites.api.*
import com.coffeepeek.domain.model.*
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ServerFavoriteBridgesTest {
    private val address = PublicAddress("canonical", "/coffee-shop/canonical", 1, false)
    private fun entry(kind: FavoriteKind = FavoriteKind.CoffeeShop) =
        FavoriteEntry(kind, address, name = "Server name", coverPhotoUrl = "https://fixture.invalid/cover")
    private class Server : FavoritesRepository {
        val state = MutableStateFlow(Result.success(emptyList<FavoriteEntry>()))
        val writes = mutableListOf<Pair<FavoriteEntry, Boolean>>()
        var reads = 0
        var observations = 0
        var clears = 0
        var afterRead: () -> Unit = {}
        override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = flow {
            observations++; emitAll(state)
        }
        override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> {
            reads++; return state.value.also { afterRead() }
        }
        override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean) =
            setFavorite(FavoriteEntry(kind, address), favorite)
        override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
            writes += entry to favorite
            return Result.success(Unit)
        }
        override fun clearCache() { clears++ }
    }

    @Test fun serverRowsKeepCanonicalNavigationAndAddresslessFallbackWithoutAFalseRoute() = runTest {
        val server = Server().apply { state.value = Result.success(listOf(entry(), entry(FavoriteKind.Roaster),
            entry().copy(address = null, name = "Removed shop", occurrence = 2))) }
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions()) {
            Result.failure(IllegalStateException("Details unavailable"))
        }
        val rows = repo.read().getOrThrow()
        assertEquals(2, rows.size)
        assertEquals("canonical", rows.first().id)
        assertEquals("canonical", rows.first().publicSlug)
        assertEquals("Server name", rows.first().title)
        assertEquals("https://fixture.invalid/cover", rows.first().photoUrl)
        assertNull(rows.last().publicSlug)
    }

    @Test fun readFailureIsNotAnEmptySuccessAndCancellationIsNotSwallowed() = runTest {
        val server = Server()
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions()) { error("No detail") }
        server.state.value = Result.failure(IllegalStateException("Offline"))
        assertTrue(repo.read().isFailure)
        server.state.value = Result.failure(CancellationException("Cancelled"))
        assertFailsWith<CancellationException> { repo.read() }
    }

    @Test fun membershipDoesNotHydrateCardsAndKeepsOnlyShopPublicSlugs() = runTest {
        val server = Server().apply { state.value = Result.success(listOf(entry(), entry(FavoriteKind.Roaster), entry().copy(address = null))) }
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions()) { error("Membership must not request details") }
        assertEquals(setOf("canonical"), repo.observeIds().first().getOrThrow())
        assertEquals(0, server.reads)
    }

    @Test fun guestObservationDoesNotOpenTheServerReader() = runTest {
        val server = Server()
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions(null)) { error("No detail") }
        val results = mutableListOf<Result<List<FavoriteShop>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.observe().collect { results += it } }
        runCurrent()
        assertEquals(emptyList(), results.last().getOrThrow())
        assertEquals(0, server.observations)
        assertEquals(0, server.reads)
    }

    @Test fun accountReplacementClearsImmediatelyAndRejectsNonCooperativeDetail() = runTest {
        val sessions = CheckInTestSessions()
        val server = Server().apply { state.value = Result.success(listOf(entry())) }
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val repo = ServerFavoriteShopsRepository(server, sessions) {
            withContext(NonCancellable) { started.complete(Unit); gate.await() }
            Result.success(CoffeeShopDetails(CoffeeShop("canonical", "Private old row", null,
                cityName = null, priceRange = null, photoUrl = null, publicAddress = address)))
        }
        val results = mutableListOf<Result<List<FavoriteShop>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.observe().collect { results += it } }
        runCurrent(); assertTrue(started.isCompleted)
        server.state.value = Result.success(emptyList())
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        assertEquals(emptyList(), results.last().getOrThrow())
        gate.complete(Unit); runCurrent()
        assertTrue(results.none { it.getOrNull()?.any { row -> row.title == "Private old row" } == true })
    }

    @Test fun removeUsesServerAddressAndLogoutOnlyClearsMemory() = runTest {
        val server = Server().apply { state.value = Result.success(listOf(entry())) }
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions()) { error("No detail on removal") }
        repo.remove("canonical").getOrThrow()
        assertEquals(listOf(entry() to false), server.writes)
        repo.clear().getOrThrow()
        assertEquals(1, server.clears)
        assertEquals(1, server.writes.size)
    }

    @Test fun ownerChangeBeforeRemovalPreventsTheServerMutation() = runTest {
        val sessions = CheckInTestSessions()
        val server = Server().apply {
            state.value = Result.success(listOf(entry()))
            afterRead = { sessions.applySession(Session("other", userId = "other")) }
        }
        val repo = ServerFavoriteShopsRepository(server, sessions) { error("No detail") }
        assertFailsWith<CancellationException> { repo.remove("canonical") }
        assertTrue(server.writes.isEmpty())
    }

    @Test fun saveResolvesTheServerAddressInsteadOfInventingAPathFromTheLocalId() = runTest {
        val server = Server()
        val repo = ServerFavoriteShopsRepository(server, CheckInTestSessions()) {
            assertEquals("old-alias", it)
            Result.success(CoffeeShopDetails(CoffeeShop("guid", "Canonical title", null,
                cityName = null, priceRange = null, photoUrl = null, publicAddress = address)))
        }
        repo.save(FavoriteShop("old-alias", "Old title")).getOrThrow()
        assertEquals(address, server.writes.single().first.address)
        assertEquals("Canonical title", server.writes.single().first.name)
        assertTrue(server.writes.single().second)
    }

    @Test fun drinksRetainOnlyTheirContractedRemovalKeysAndCannotBecomeShopRoutes() = runTest {
        val server = Server().apply { state.value = Result.success(listOf(entry(),
            FavoriteEntry(FavoriteKind.Drink, drinkSlug = "latte", name = "Latte"),
            FavoriteEntry(FavoriteKind.Drink, name = "No target", occurrence = 2))) }
        val repo = ServerFavoriteDrinksRepository(server, CheckInTestSessions())
        val rows = repo.observe().first().getOrThrow()
        assertEquals(2, rows.size)
        assertTrue(rows.first().canRemove)
        assertFalse(rows.last().canRemove)
        repo.remove(rows.first().id).getOrThrow()
        assertEquals("latte", server.writes.single().first.drinkSlug)
        assertFalse(server.writes.single().second)
        server.state.value = Result.failure(IllegalStateException("Offline"))
        assertTrue(repo.observe().first().isFailure)
    }

    @Test fun roasterServerErrorsAreVisibleToTheMigratedScreen() = runTest {
        val errors = MutableStateFlow<String?>(null)
        val roasters = object : RoasterFavorites {
            override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
            override fun observeErrors() = errors
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("No write")
        }
        val repo = LegacyFavoriteRoastersRepository(roasters, CheckInTestSessions()) { error("No photo") }
        val results = mutableListOf<Result<List<com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.observe().collect { results += it } }
        runCurrent(); errors.value = "Server unavailable"; runCurrent()
        assertTrue(results.last().isFailure)
    }
}
