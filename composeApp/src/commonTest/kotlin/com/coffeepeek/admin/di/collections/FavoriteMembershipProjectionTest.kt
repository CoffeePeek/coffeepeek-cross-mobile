package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.analytics.RecordingTransport
import com.coffeepeek.admin.analytics.analytics
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteMembershipProjectionTest {
    private val target = SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "shop")
    private val membership = FavoriteTarget(FavoriteKind.CoffeeShop, "shop")

    @Test fun actualRemovalAppliesTheConfirmedFavoriteWithoutAnyReadOrWriter() = runTest {
        val saved = Saved()
        val favorites = Favorites()
        val wrapper = FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
        wrapper.changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
        assertEquals(listOf(membership to false), favorites.applied)
        assertEquals(0, favorites.reads)
        assertEquals(1, saved.writes)
    }

    @Test fun remainingMembershipKeepsTheHeartWhenOnlyOneCollectionIsUnchecked() = runTest {
        val saved = Saved().apply {
            result = Result.success(SavedCollectionMembershipResult(listOf(SavedCollectionSummary("other", "Other", containsItem = true))))
        }
        val favorites = Favorites()
        FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
            .changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
        assertEquals(listOf(membership to true), favorites.applied)
        assertEquals(0, favorites.reads)
    }

    @Test fun noOpDoesNotPrepareAnalyticsCallTheDelegateOrReadFavorites() = runTest {
        val saved = Saved().apply { baseline = baseline.map { it.copy(containsItem = false) } }
        val favorites = Favorites()
        val result = FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
            .changeMemberships(target, emptySet(), emptySet()).getOrThrow()
        assertEquals(saved.baseline, result.lists)
        assertEquals(false, result.membershipChanged)
        assertEquals(0, saved.writes)
        assertEquals(0, favorites.reads)
        assertTrue(favorites.applied.isEmpty())
        assertEquals(true, favorites.member)
    }

    @Test fun failedWriteDoesNotChangeTheCardProjection() = runTest {
        val saved = Saved().apply { result = Result.failure(IllegalStateException("offline")) }
        val favorites = Favorites()
        assertTrue(FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
            .changeMemberships(target, emptySet(), setOf("default")).isFailure)
        assertTrue(favorites.applied.isEmpty())
        assertEquals(0, favorites.reads)
    }

    @Test fun aDelayedPutDoesNotPublishAnUnconfirmedRemoval() = runTest {
        val saved = Saved().apply { gate = CompletableDeferred() }
        val favorites = Favorites()
        val wrapper = FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
        val request = async { wrapper.changeMemberships(target, emptySet(), setOf("default")) }
        runCurrent()
        assertTrue(favorites.applied.isEmpty())
        saved.gate!!.complete(Unit)
        request.await().getOrThrow()
        assertEquals(listOf(membership to false), favorites.applied)
    }

    @Test fun replacedSameAccountSessionCannotReceiveTheOldPutResult() = runTest {
        val saved = Saved().apply { gate = CompletableDeferred() }
        val favorites = Favorites()
        val sessions = Sessions()
        val wrapper = FavoriteMembershipAnalyticsRepository(saved, favorites, sessions)
        val request = async { wrapper.changeMemberships(target, emptySet(), setOf("default")) }
        runCurrent()
        sessions.generation++
        saved.gate!!.complete(Unit)
        assertFailsWith<CancellationException> { request.await() }
        assertTrue(favorites.applied.isEmpty())
    }

    @Test fun staleRemovalActualNoOpKeepsLegacyHeartWithoutCreditingAMutation() = runTest {
        withAnalytics { transport, _ ->
            val saved = Saved().apply { result = Result.success(SavedCollectionMembershipResult(emptyList(), true, false)) }
            val favorites = Favorites()
            FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
                .changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
            runCurrent()
            assertEquals(true, favorites.member)
            assertEquals(listOf(membership to true), favorites.applied)
            assertEquals(listOf(AnalyticsEvent.FAVORITE_ATTEMPTED.wire), transport.events.map { it.first })
        }
    }

    @Test fun actualLastMembershipRemovalClearsHeartAndCreditsOnlyConfirmedChange() = runTest {
        withAnalytics { transport, _ ->
            val favorites = Favorites()
            FavoriteMembershipAnalyticsRepository(Saved(), favorites, Sessions())
                .changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
            runCurrent()
            assertEquals(false, favorites.member)
            val changed = transport.events.single { it.first == AnalyticsEvent.FAVORITE_CHANGED.wire }
            assertEquals(false, changed.second["is_favorite"])
        }
    }

    @Test fun missingMetadataAndEmptyUnionDoNotInferFalseOrCreditAMutation() = runTest {
        withAnalytics { transport, _ ->
            val saved = Saved().apply { result = Result.success(SavedCollectionMembershipResult(emptyList())) }
            val favorites = Favorites()
            FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
                .changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
            runCurrent()
            assertTrue(favorites.applied.isEmpty())
            assertEquals(true, favorites.member)
            assertEquals(listOf(AnalyticsEvent.FAVORITE_ATTEMPTED.wire), transport.events.map { it.first })
        }
    }

    @Test fun unknownActualChangeCanUpdateTheHeartButCannotCreditAMutation() = runTest {
        withAnalytics { transport, _ ->
            val saved = Saved().apply { result = Result.success(SavedCollectionMembershipResult(emptyList(), isFavorite = false)) }
            val favorites = Favorites()
            FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
                .changeMemberships(target, emptySet(), setOf("default")).getOrThrow()
            runCurrent()
            assertEquals(false, favorites.member)
            assertEquals(listOf(AnalyticsEvent.FAVORITE_ATTEMPTED.wire), transport.events.map { it.first })
        }
    }

    @Test fun emptyRequestEmitsNoAnalyticsEvenWhenCollectionFlagsDisagreeWithGlobalHeart() = runTest {
        withAnalytics { transport, _ ->
            val saved = Saved().apply { baseline = emptyList() }
            val favorites = Favorites()
            FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
                .changeMemberships(target, emptySet(), emptySet()).getOrThrow()
            runCurrent()
            assertTrue(transport.events.isEmpty())
            assertEquals(0, saved.writes)
            assertTrue(favorites.applied.isEmpty())
            assertEquals(true, favorites.member)
        }
    }

    @Test fun failedWriteRetainsAttemptAndFailureAnalyticsWithoutCreditingAMutation() = runTest {
        withAnalytics { transport, _ ->
            val saved = Saved().apply { result = Result.failure(IllegalStateException("offline")) }
            FavoriteMembershipAnalyticsRepository(saved, Favorites(), Sessions())
                .changeMemberships(target, emptySet(), setOf("default"))
            runCurrent()
            assertEquals(listOf(AnalyticsEvent.FAVORITE_ATTEMPTED.wire, AnalyticsEvent.FAVORITE_FAILED.wire), transport.events.map { it.first })
        }
    }

    @Test fun consentRevokedDuringWritePreventsChangedAnalyticsButKeepsConfirmedProjection() = runTest {
        withAnalytics { transport, service ->
            val saved = Saved().apply { gate = CompletableDeferred() }
            val favorites = Favorites()
            val wrapper = FavoriteMembershipAnalyticsRepository(saved, favorites, Sessions())
            val request = async { wrapper.changeMemberships(target, emptySet(), setOf("default")) }
            runCurrent()
            service.setConsent(AnalyticsConsent.Declined)
            saved.gate!!.complete(Unit)
            request.await().getOrThrow()
            runCurrent()
            assertEquals(false, favorites.member)
            assertTrue(transport.events.none { it.first == AnalyticsEvent.FAVORITE_CHANGED.wire })
        }
    }

    private suspend fun TestScope.withAnalytics(test: suspend (RecordingTransport, AnalyticsService) -> Unit) {
        val previous = Analytics.service
        val transport = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), transport)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity("owner")
        Analytics.install(service)
        try { test(transport, service) }
        finally {
            service.setForeground(false)
            previous?.let(Analytics::install)
        }
    }

    private class Sessions : SessionReader {
        var generation = 1L
        private val state = MutableStateFlow<Session?>(Session("token", userId = "owner"))
        override fun accountGeneration() = generation
        override fun peekSession() = state.value
        override fun isActiveSession(session: Session?) = session != null
        override fun observeSession() = state
        override suspend fun getSession() = state.value
    }

    private class Favorites : AccountFavorites {
        var reads = 0
        var member: Boolean? = true
        val applied = mutableListOf<Pair<FavoriteTarget, Boolean>>()
        override fun observeFavorites() = flowOf(Result.success(emptyList<FavoriteEntry>()))
        override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> {
            reads++
            error("Metadata reads must not delay confirmation")
        }
        override fun cachedMembership(target: FavoriteTarget) = member
        override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean) = error("Projection never writes favorites")
        override fun applyConfirmedMembership(expectedAccount: String, expectedGeneration: Long, target: FavoriteTarget, favorite: Boolean): Boolean {
            assertEquals("owner", expectedAccount)
            assertEquals(1L, expectedGeneration)
            applied += target to favorite
            member = favorite
            return true
        }
        override fun clearCache() = Unit
    }

    private class Saved : SavedCollectionsRepository {
        var baseline = listOf(SavedCollectionSummary("default", "Favorites", containsItem = true))
        var result: Result<SavedCollectionMembershipResult> = Result.success(
            SavedCollectionMembershipResult(baseline.map { it.copy(containsItem = false) }, isFavorite = false, membershipChanged = true),
        )
        var gate: CompletableDeferred<Unit>? = null
        var writes = 0
        override fun cachedMine(target: SavedCollectionTarget?) = baseline
        override suspend fun changeMemberships(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): Result<SavedCollectionMembershipResult> {
            writes++
            gate?.await()
            return result
        }
        override suspend fun mine(target: SavedCollectionTarget?) = error("Only cached baseline is needed")
        override suspend fun owned(id: String) = error("Unused")
        override suspend fun public(id: String) = error("Unused")
        override suspend fun create(metadata: SavedCollectionMetadata) = error("Unused")
        override suspend fun update(id: String, metadata: SavedCollectionMetadata, revision: Long) = error("Unused")
        override suspend fun replaceItems(id: String, places: List<CollectionPlace>, revision: Long) = error("Unused")
        override suspend fun memberships(target: SavedCollectionTarget, collectionIds: Set<String>) = error("Unused")
        override suspend fun delete(id: String) = error("Unused")
        override suspend fun featuredId() = error("Unused")
        override suspend fun setFeatured(id: String?) = error("Unused")
        override suspend fun featured(profileSlug: String) = error("Unused")
        override suspend fun uploadCover(input: CollectionCoverInput) = error("Unused")
        override suspend fun cover(collectionId: String, cover: SavedCollectionCover) = error("Unused")
    }
}
