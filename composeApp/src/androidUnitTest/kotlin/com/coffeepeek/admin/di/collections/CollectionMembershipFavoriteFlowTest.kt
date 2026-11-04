package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.di.discovery.AppDiscoveryFavorites
import com.coffeepeek.admin.di.favorites.ServerFavoriteShopsRepository
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.admin.sandbox.backend.SandboxSavedCollectionsBackend
import com.coffeepeek.api.feature.favorites.FavoritesApiService
import com.coffeepeek.data.feature.favorites.ServerFavoritesRepository
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteTarget
import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.collections.data.repository.createSavedCollectionsRepository
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollectionMetadata
import com.coffeepeek.feature.collections.domain.model.SavedCollectionTarget
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpMethod
import java.io.Closeable
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionMembershipFavoriteFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val shop = SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "sandbox-cafe")
    private val roaster = SavedCollectionTarget(CollectionPlaceKind.Roaster, "sandbox-roaster")

    @Test fun addingANewShopUpdatesTheCardAndServerWithoutAnEagerLegacyFavoriteWrite() = runTest(dispatcher) {
        Fixture(backgroundScope).use { fixture ->
            val target = SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "sandbox-lab")
            fixture.favorites.getFavorites().getOrThrow()
            val list = fixture.saved.mine(target).getOrThrow().single { it.isDefault }
            runCurrent()
            assertFalse(target.slug in fixture.visibleShopIds)
            val reads = fixture.favoriteReads
            fixture.blockRefreshAfterWrite = true
            fixture.saved.changeMemberships(target, setOf(list.id), emptySet()).getOrThrow()
            runCurrent()
            assertTrue(target.slug in fixture.visibleShopIds)
            assertEquals(true, fixture.favorites.cachedMembership(target.favoriteTarget()))
            assertEquals(reads, fixture.favoriteReads)
            assertTrue(fixture.requests.none { it.startsWith("PUT /api/v1/favorites/") })
            fixture.rejectFavoriteReads = false
            assertTrue(fixture.favorites.getFavorites(refresh = true).getOrThrow().any { it.slug == target.slug })
        }
    }

    @Test fun aFailedMembershipPutKeepsTheCardAndVerifiedPickerStateUntilRetrySucceeds() = runTest(dispatcher) {
        Fixture(backgroundScope).use { fixture ->
            fixture.favorites.getFavorites().getOrThrow()
            val before = fixture.saved.mine(shop).getOrThrow()
            val selected = before.filter { it.containsItem }.map { it.id }.toSet()
            fixture.backend.scenario = SandboxScenario.SaveError
            assertTrue(fixture.saved.changeMemberships(shop, emptySet(), selected).isFailure)
            runCurrent()
            assertEquals(before, fixture.saved.cachedFreshMine(shop))
            assertEquals(true, fixture.favorites.cachedMembership(shop.favoriteTarget()))
            assertTrue(shop.slug in fixture.visibleShopIds)
            fixture.backend.scenario = SandboxScenario.Ready
            assertTrue(fixture.favorites.getFavorites(refresh = true).getOrThrow().any { it.slug == shop.slug })
            fixture.saved.changeMemberships(shop, emptySet(), selected).getOrThrow()
            runCurrent()
            assertFalse(shop.slug in fixture.visibleShopIds)
        }
    }

    @Test fun removingTheLastMembershipUpdatesBothTheCardProjectionAndServerFavorites() = runTest(dispatcher) {
        for (target in listOf(shop, roaster)) Fixture(backgroundScope).use { fixture ->
            fixture.favorites.getFavorites().getOrThrow()
            val lists = fixture.saved.mine(target).getOrThrow()
            runCurrent()
            val selected = lists.filter { it.containsItem }.map { it.id }.toSet()
            assertTrue(selected.isNotEmpty())
            fixture.blockRefreshAfterWrite = true
            val before = fixture.favoriteReads
            val result = fixture.saved.changeMemberships(target, emptySet(), selected).getOrThrow()
            runCurrent()
            assertTrue(result.none { it.containsItem })
            assertEquals(before, fixture.favoriteReads)
            assertEquals(false, fixture.favorites.cachedMembership(target.favoriteTarget()))
            if (target == shop) assertFalse(shop.slug in fixture.visibleShopIds)
            fixture.rejectFavoriteReads = false
            assertTrue(fixture.favorites.getFavorites(refresh = true).getOrThrow().none { it.kind == target.favoriteTarget().kind && it.slug == target.slug })
        }
    }

    @Test fun movingAndRemovingOneOfTwoMembershipsKeepsTheHeartUntilTheLastRemoval() = runTest(dispatcher) {
        Fixture(backgroundScope).use { fixture ->
            fixture.favorites.getFavorites().getOrThrow()
            val default = fixture.saved.mine(shop).getOrThrow().single { it.isDefault }
            val first = fixture.saved.create(SavedCollectionMetadata("First")).getOrThrow().summary.id
            val second = fixture.saved.create(SavedCollectionMetadata("Second")).getOrThrow().summary.id
            fixture.saved.changeMemberships(shop, setOf(first, second), setOf(default.id)).getOrThrow()
            assertEquals(true, fixture.favorites.cachedMembership(shop.favoriteTarget()))
            val remaining = fixture.saved.changeMemberships(shop, emptySet(), setOf(first)).getOrThrow()
            assertEquals(setOf(second), remaining.filter { it.containsItem }.map { it.id }.toSet())
            assertEquals(true, fixture.favorites.cachedMembership(shop.favoriteTarget()))
            fixture.saved.changeMemberships(shop, emptySet(), setOf(second)).getOrThrow()
            assertEquals(false, fixture.favorites.cachedMembership(shop.favoriteTarget()))
            runCurrent()
            assertFalse(shop.slug in fixture.visibleShopIds)
        }
    }

    @Test fun unchangedDoneAndFreshReopeningMakeNoHttpRequests() = runTest(dispatcher) {
        Fixture(backgroundScope).use { fixture ->
            fixture.favorites.getFavorites().getOrThrow()
            val first = fixture.saved.mine(shop).getOrThrow()
            runCurrent()
            val requests = fixture.requests.size
            assertEquals(first, fixture.saved.changeMemberships(shop, emptySet(), emptySet()).getOrThrow())
            assertEquals(first, fixture.saved.cachedFreshMine(shop))
            assertEquals(first, fixture.saved.mine(shop).getOrThrow())
            runCurrent()
            assertEquals(requests, fixture.requests.size)
            assertTrue(shop.slug in fixture.visibleShopIds)
        }
    }

    @Test fun aFailedBackgroundRefreshCannotResurrectTheRemovedHeart() = runTest(dispatcher) {
        Fixture(backgroundScope).use { fixture ->
            fixture.favorites.getFavorites().getOrThrow()
            val selected = fixture.saved.mine(shop).getOrThrow().filter { it.containsItem }.map { it.id }.toSet()
            val sync = backgroundScope.launch { synchronizeCollectionFavorites(fixture.saved.observeChanges(), fixture.favorites) }
            runCurrent()
            fixture.blockRefreshAfterWrite = true
            fixture.saved.changeMemberships(shop, emptySet(), selected).getOrThrow()
            runCurrent()
            assertFalse(shop.slug in fixture.visibleShopIds)
            assertEquals(false, fixture.favorites.cachedMembership(shop.favoriteTarget()))
            sync.cancel()
        }
    }

    private inner class Fixture(scope: CoroutineScope) : Closeable {
        val backend = SandboxBackend(byteArrayOf(1), engineDispatcher = dispatcher)
        private val owner = SandboxSavedCollectionsBackend.LOCAL_ACCOUNT
        private val sessions = CheckInTestSessions(Session("sandbox-only", userId = owner))
        val favorites = ServerFavoritesRepository(FavoritesApiService(backend.gateway), sessions)
        private val account = object : CollectionsAccount {
            override val identities = MutableStateFlow<String?>(owner)
            override fun currentId() = owner
            override suspend fun loadId() = owner
        }
        private val raw = createSavedCollectionsRepository(backend.gateway, backend.gateway, SandboxBackend.BASE_URL, account) { request, owner ->
            request.headers.append(SandboxSavedCollectionsBackend.OWNER, owner)
        }
        val saved = FavoriteMembershipAnalyticsRepository(raw, favorites, sessions)
        private val discovery = AppDiscoveryFavorites(ServerFavoriteShopsRepository(favorites, sessions) { error("IDs must not load details") })
        var visibleShopIds = emptySet<String>()
        private val observation = scope.launch { discovery.ids.collect { visibleShopIds = it } }
        var favoriteReads = 0
        val requests = mutableListOf<String>()
        var rejectFavoriteReads = false
        var blockRefreshAfterWrite = false

        init {
            backend.gateway.plugin(HttpSend).intercept { request ->
                val path = request.url.build().encodedPath
                requests += "${request.method.value} $path"
                if (path == "/api/v1/favorites" && request.method == HttpMethod.Get) {
                    favoriteReads++
                    if (rejectFavoriteReads) throw IOException("Fixture favorite refresh unavailable")
                }
                val response = execute(request)
                if (path.startsWith("/api/v1/saved-collections/membership/") && request.method == HttpMethod.Put && blockRefreshAfterWrite)
                    rejectFavoriteReads = true
                response
            }
        }

        override fun close() {
            observation.cancel()
            backend.close()
        }
    }

    private fun SavedCollectionTarget.favoriteTarget() =
        FavoriteTarget(if (kind == CollectionPlaceKind.CoffeeShop) FavoriteKind.CoffeeShop else FavoriteKind.Roaster, slug)
}
