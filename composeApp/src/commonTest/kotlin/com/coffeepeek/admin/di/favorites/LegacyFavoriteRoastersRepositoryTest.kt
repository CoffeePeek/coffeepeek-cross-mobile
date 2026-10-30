package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyFavoriteRoastersRepositoryTest {
    private class Store : RoasterFavorites {
        val rows = MutableStateFlow<List<CatalogItem>>(emptyList())
        val removed = mutableListOf<Pair<String, String>>()
        var failure: Throwable? = null
        override fun observeFavorites() = rows
        override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("not used")
        override suspend fun removeForAccount(roaster: CatalogItem, expectedUserId: String): Result<Unit> {
            failure?.let { return Result.failure(it) }
            removed += roaster.id to expectedUserId
            rows.value = rows.value.filterNot { it.address?.slug == roaster.address?.slug && it.id == roaster.id }
            return Result.success(Unit)
        }
    }

    private fun roaster(slug: String) = CatalogItem("catalog-$slug", "Roaster $slug",
        address = PublicAddress(slug, "/roasters/$slug", 1, false), coffeeShopsCount = 7, coffeeProductsCount = 12)

    @Test fun savedMetadataUsesCanonicalIdentityAndNeverInventsMissingAddress() = runTest {
        val store = Store().apply { rows.value = listOf(roaster("a").copy(photoUrl = "saved.png"),
            CatalogItem("opaque-id", "No address", slug = "not-a-public-address")) }
        val loads = mutableListOf<String>()
        val source = LegacyFavoriteRoastersRepository(store, CheckInTestSessions()) {
            loads += it; Result.success("fallback.png")
        }
        val emissions = mutableListOf<Result<List<FavoriteRoaster>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect { emissions += it } }
        runCurrent()
        val rows = emissions.last().getOrThrow()
        assertEquals("a", rows.first().id); assertEquals("a", rows.first().publicSlug)
        assertEquals(7, rows.first().coffeeShopsCount); assertEquals(12, rows.first().coffeeProductsCount)
        assertEquals("saved.png", rows.first().photoUrl); assertNull(rows.last().publicSlug)
        assertTrue(loads.isEmpty())
    }

    @Test fun fallbackPhotosAreBoundedAndFailedReadsKeepSavedCards() = runTest {
        val store = Store().apply { rows.value = (0..6).map { roaster(it.toString()) } }
        val gate = CompletableDeferred<Unit>()
        var inFlight = 0; var peak = 0
        val source = LegacyFavoriteRoastersRepository(store, CheckInTestSessions()) { slug ->
            inFlight++; peak = maxOf(peak, inFlight)
            try {
                gate.await()
                if (slug == "0") Result.failure(IllegalStateException("Photo unavailable"))
                else Result.success("photo-$slug.png")
            } finally { inFlight-- }
        }
        val emissions = mutableListOf<Result<List<FavoriteRoaster>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect { emissions += it } }
        runCurrent(); assertEquals(4, inFlight); assertEquals(7, emissions.last().getOrThrow().size)
        gate.complete(Unit); runCurrent()
        assertEquals(4, peak)
        assertTrue(emissions.all { it.isSuccess })
        val rows = emissions.last().getOrThrow()
        assertNull(rows.first().photoUrl); assertEquals("photo-6.png", rows.last().photoUrl)
    }

    @Test fun logoutClearsRowsBeforeNonCooperativeOldPhotoCompletes() = runTest {
        val sessions = CheckInTestSessions()
        val store = Store().apply { rows.value = listOf(roaster("a")) }
        val gate = CompletableDeferred<Unit>()
        val source = LegacyFavoriteRoastersRepository(store, sessions) {
            withContext(NonCancellable) { gate.await() }; Result.success("private-old.png")
        }
        val emissions = mutableListOf<Result<List<FavoriteRoaster>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect { emissions += it } }
        runCurrent(); assertEquals(1, emissions.last().getOrThrow().size)
        sessions.applySession(null); runCurrent()
        assertTrue(emissions.last().getOrThrow().isEmpty())
        val afterLogout = emissions.size
        gate.complete(Unit); runCurrent()
        assertTrue(emissions.drop(afterLogout).none { it.getOrNull()?.isNotEmpty() == true })
    }

    @Test fun removalBindsOriginalOwnerAndCancellationNeverBecomesFailure() = runTest {
        val sessions = CheckInTestSessions()
        val store = Store().apply { rows.value = listOf(roaster("a").copy(photoUrl = "saved.png")) }
        val source = LegacyFavoriteRoastersRepository(store, sessions) { error("Saved logo needs no fetch") }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect {} }
        runCurrent()
        val failure = IllegalStateException("Disk full")
        store.failure = failure
        assertSame(failure, source.remove("a").exceptionOrNull())
        store.failure = CancellationException("Cancelled write")
        assertFailsWith<CancellationException> { source.remove("a") }
        store.failure = null
        source.remove("a").getOrThrow()
        assertEquals(listOf("catalog-a" to "owner"), store.removed)
    }

    @Test fun synchronousAccountReplacementRejectsOldActionsBeforeObserverRuns() = runTest {
        val sessions = CheckInTestSessions()
        val store = Store().apply { rows.value = listOf(roaster("a").copy(photoUrl = "saved.png")) }
        val source = LegacyFavoriteRoastersRepository(store, sessions) { error("not used") }
        backgroundScope.launch { source.observe().collect {} }
        runCurrent()
        sessions.applySession(Session("b-token", userId = "other"))
        assertFailsWith<CancellationException> { source.remove("a") }
        assertTrue(store.removed.isEmpty())
    }

    @Test fun tokenRenewalDoesNotRestartCatalogueReads() = runTest {
        val sessions = CheckInTestSessions()
        val store = Store().apply { rows.value = listOf(roaster("a")) }
        var reads = 0
        val source = LegacyFavoriteRoastersRepository(store, sessions) { reads++; Result.success("logo.png") }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect {} }
        runCurrent(); assertEquals(1, reads)
        sessions.applySession(Session("renewed-token", userId = "owner")); runCurrent()
        assertEquals(1, reads)
    }

    @Test fun successfulPhotoFallbackIsReusedWhenAnotherFavoriteChanges() = runTest {
        val store = Store().apply { rows.value = listOf(roaster("a")) }
        val reads = mutableListOf<String>()
        val source = LegacyFavoriteRoastersRepository(store, CheckInTestSessions()) { slug ->
            reads += slug; Result.success("$slug.png")
        }
        val emissions = mutableListOf<Result<List<FavoriteRoaster>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.observe().collect { emissions += it } }
        runCurrent(); assertEquals(listOf("a"), reads)
        store.rows.value = store.rows.value + roaster("b"); runCurrent()
        assertEquals(listOf("a", "b"), reads)
        assertEquals(listOf("a.png", "b.png"), emissions.last().getOrThrow().map { it.photoUrl })
        store.rows.value = listOf(roaster("b")); runCurrent()
        store.rows.value = listOf(roaster("b"), roaster("a")); runCurrent()
        assertEquals(listOf("a", "b", "a"), reads)
    }

    @Test fun sessionObservationFailureClearsPrivateRowsBeforeReportingError() = runTest {
        val currentSession = CheckInTestSessions()
        val failureGate = CompletableDeferred<Unit>()
        val sessions = object : SessionStore by currentSession {
            override fun observeSession() = flow {
                emit(currentSession.peekSession())
                failureGate.await()
                throw IllegalStateException("Session observation unavailable")
            }
        }
        val store = Store().apply { rows.value = listOf(roaster("a").copy(photoUrl = "saved.png")) }
        val source = LegacyFavoriteRoastersRepository(store, sessions) { error("not used") }
        val emissions = mutableListOf<Result<List<FavoriteRoaster>>>()
        val collectorGate = CompletableDeferred<Unit>()
        backgroundScope.launch { source.observe().collect {
            emissions += it
            if (it.getOrNull()?.isNotEmpty() == true) collectorGate.await()
        } }
        runCurrent(); assertEquals(1, emissions.last().getOrThrow().size)
        val beforeFailure = emissions.size
        failureGate.complete(Unit); runCurrent()
        collectorGate.complete(Unit); runCurrent()
        val afterFailure = emissions.drop(beforeFailure)
        assertTrue(afterFailure.first().getOrThrow().isEmpty())
        assertTrue(afterFailure.last().isFailure)
    }
}
