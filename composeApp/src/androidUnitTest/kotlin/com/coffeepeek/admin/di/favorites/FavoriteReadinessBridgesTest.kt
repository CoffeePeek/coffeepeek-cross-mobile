package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.roaster.api.PublicRoasterDescription
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterSummary
import com.coffeepeek.feature.shop.api.PublicShopSummary
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteReadinessBridgesTest {
    private val address = FavoriteAddress("canonical", "/coffee-shop/canonical", 1, false)

    @Test fun aTechnicalShopEmptyDoesNotSettleBeforeActualCardsAreHydrated() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.CoffeeShop)))
        val gate = CompletableDeferred<Unit>()
        val source = ServerFavoriteShopsRepository(server, CheckInTestSessions()) {
            gate.await()
            Result.success(details())
        }
        val results = record(source.observe())
        runCurrent()
        assertTrue(results.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals("Server saved", results.single().getOrThrow().single().title)
    }

    @Test fun aTechnicalRoasterEmptyDoesNotSettleBeforeActualCatalogueRows() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.Roaster)))
        val gate = CompletableDeferred<Unit>()
        val sessions = CheckInTestSessions()
        val favorites = ServerRoasterFavorites(server, catalogue { gate.await() }, sessions)
        val source = AccountFavoriteRoasters(favorites, sessions) { error("Saved photo requires no fallback") }
        val results = record(source.observe())
        runCurrent()
        assertTrue(results.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals("Server saved", results.single().getOrThrow().single().title)
    }

    @Test fun aTechnicalDrinkEmptyWaitsForTheActualServerRead() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.Drink)))
        val gate = CompletableDeferred<Unit>()
        server.beforeRead = { gate.await() }
        val source = ServerFavoriteDrinksRepository(server, CheckInTestSessions())
        val results = record(source.observe())
        runCurrent()
        assertTrue(results.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals("Server saved", results.single().getOrThrow().single().title)
    }

    @Test fun anActuallyEmptyServerListSettlesNormally() = runTest {
        val server = PendingServer(emptyList())
        val sessions = CheckInTestSessions()
        val shops = ServerFavoriteShopsRepository(server, sessions) { error("Empty list has no details") }
        val drinks = ServerFavoriteDrinksRepository(server, sessions)
        val roasters = AccountFavoriteRoasters(ServerRoasterFavorites(server, catalogue(), sessions), sessions) { error("No photo") }
        assertTrue(shops.observe().first().getOrThrow().isEmpty())
        assertTrue(drinks.observe().first().getOrThrow().isEmpty())
        assertTrue(roasters.observe().first().getOrThrow().isEmpty())
    }

    @Test fun sameOwnerReobservationNeverEmitsAnInventedEmptyListBeforeCachedRows() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.CoffeeShop)))
        val source = ServerFavoriteShopsRepository(server, CheckInTestSessions()) { Result.success(details()) }
        repeat(2) { assertEquals("canonical", source.observe().first().getOrThrow().single().id) }
    }

    @Test fun normalizedDuplicateSignalsDoNotRepeatExpensiveShopHydration() = runTest {
        val saved = entry(FavoriteKind.CoffeeShop)
        val server = PendingServer(listOf(saved))
        var hydrations = 0
        val source = ServerFavoriteShopsRepository(server, CheckInTestSessions()) {
            hydrations++
            Result.success(details())
        }
        val results = record(source.observe())
        runCurrent()
        server.signals.value = Result.success(listOf(saved))
        runCurrent()
        assertEquals(1, hydrations)
        assertEquals(1, results.size)
    }

    @Test fun normalizedDuplicateSignalsDoNotRepeatRoasterCatalogueHydration() = runTest {
        val saved = entry(FavoriteKind.Roaster)
        val server = PendingServer(listOf(saved))
        var reads = 0
        val source = ServerRoasterFavorites(server, catalogue { reads++ }, CheckInTestSessions())
        val results = record(source.observeFavorites())
        runCurrent()
        server.signals.value = Result.success(listOf(saved))
        runCurrent()
        assertEquals(1, reads)
        assertEquals(1, results.size)
    }

    @Test fun anActualReadFailureRemainsAFailureInsteadOfAnEmptySettledList() = runTest {
        val failure = IllegalStateException("Unavailable")
        val server = PendingServer(emptyList()).apply { actual = Result.failure(failure) }
        val source = ServerFavoriteShopsRepository(server, CheckInTestSessions()) { error("No details after failure") }
        assertSame(failure, source.observe().first().exceptionOrNull())
    }

    @Test fun normalizationPreservesRawFailuresAndRethrowsCancellation() = runTest {
        val failure = IllegalStateException("Raw failure")
        val server = PendingServer(emptyList()).apply { signals.value = Result.failure(failure) }
        val sessions = CheckInTestSessions()
        assertSame(failure, observeLoadedServerFavorites(server, sessions).first().exceptionOrNull())
        assertEquals(0, server.reads)
        server.signals.value = Result.failure(CancellationException("Cancelled"))
        assertFailsWith<CancellationException> { observeLoadedServerFavorites(server, sessions).first() }
        server.signals.value = Result.success(emptyList())
        server.actual = Result.failure(CancellationException("Read cancelled"))
        assertFailsWith<CancellationException> { observeLoadedServerFavorites(server, sessions).first() }
    }

    @Test fun logoutClearsDrinksBeforeANonCooperativeOldReadCompletes() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.Drink)))
        val gate = CompletableDeferred<Unit>()
        val sessions = CheckInTestSessions()
        server.beforeRead = { withContext(NonCancellable) { gate.await() } }
        val results = record(ServerFavoriteDrinksRepository(server, sessions).observe())
        runCurrent()
        assertTrue(results.isEmpty())
        sessions.applySession(null)
        runCurrent()
        assertTrue(results.last().getOrThrow().isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertTrue(results.all { it.getOrNull()?.isEmpty() == true })
    }

    @Test fun commonRoasterReadRetainsImmediateOwnerRevocationForLegacyConsumers() = runTest {
        val server = PendingServer(listOf(entry(FavoriteKind.Roaster)))
        val gate = CompletableDeferred<Unit>()
        val sessions = CheckInTestSessions()
        val source = ServerRoasterFavorites(server, catalogue { withContext(NonCancellable) { gate.await() } }, sessions)
        val results = record(source.observeFavorites())
        runCurrent()
        assertTrue(results.isEmpty())
        sessions.applySession(null)
        runCurrent()
        assertTrue(results.last().isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertTrue(results.all { it.isEmpty() })
    }

    @Test fun sessionAccountIgnoresTokenRenewalButPublishesAccountRevocation() = runTest {
        val sessions = CheckInTestSessions()
        val account = SessionFavoritesAccount(sessions)
        val owners = record(account.identities)
        runCurrent()
        sessions.applySession(Session("rotated", userId = "owner"))
        runCurrent()
        assertEquals(listOf<String?>("owner"), owners)
        sessions.applySession(null)
        assertNull(account.currentId())
        runCurrent()
        assertEquals(listOf("owner", null), owners)
    }

    private fun entry(kind: FavoriteKind) = FavoriteEntry(kind, address.takeUnless { kind == FavoriteKind.Drink },
        drinkSlug = "latte".takeIf { kind == FavoriteKind.Drink }, name = "Server saved", coverPhotoUrl = "https://fixture.invalid/card")

    private fun details() = PublicShopSummary(null, null, null, null, 0, false, emptyList(),
        slug = address.slug, canonicalPath = address.canonicalPath, revision = address.revision, isAlias = address.isAlias, title = "Details")

    private fun catalogue(beforeRead: suspend () -> Unit = {}) = object : PublicRoasterDetails {
        override suspend fun summaries(): Result<List<PublicRoasterSummary>> {
            beforeRead()
            return Result.success(listOf(PublicRoasterSummary(address.slug, "https://fixture.invalid/logo", emptyList(), 0, 0, 0, "Catalogue")))
        }
        override suspend fun load(slug: String): Result<PublicRoasterDescription> = error("Catalogue already contains saved roaster")
    }

    private fun <T> TestScope.record(flow: Flow<T>): MutableList<T> = mutableListOf<T>().also { values ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { values += it } }
    }

    private class PendingServer(items: List<FavoriteEntry>) : AccountFavorites {
        val signals = MutableStateFlow<Result<List<FavoriteEntry>>>(Result.success(emptyList()))
        var actual = Result.success(items)
        var reads = 0
        var beforeRead: suspend () -> Unit = {}
        override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = signals
        override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> {
            val captured = actual
            reads++
            beforeRead()
            return captured
        }
        override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> = error("No write")
        override fun clearCache() = Unit
    }
}
