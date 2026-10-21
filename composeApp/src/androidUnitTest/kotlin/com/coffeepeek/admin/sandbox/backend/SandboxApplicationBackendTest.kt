package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.api.feature.favorites.FavoritesApiService
import com.coffeepeek.api.feature.feed.FeedApiService
import com.coffeepeek.api.service.*
import com.coffeepeek.api.model.request.ShopSearchRequestDto
import com.coffeepeek.api.model.request.ShopSearchFiltersDto
import com.coffeepeek.api.model.request.ShopSearchOriginDto
import com.coffeepeek.admin.feature.coffee.data.CoffeeRepositoryImpl
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.profile.data.PublicProfileRepositoryImpl
import com.coffeepeek.admin.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.domain.feature.feed.FeedRepository
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedPage
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.domain.model.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import kotlin.test.*

class SandboxApplicationBackendTest {
    @get:Rule val temporary = TemporaryFolder()
    private suspend fun fixture(block: suspend (SandboxBackend, CoffeePeekClient) -> Unit) {
        val backend = SandboxBackend(byteArrayOf(1, 2, 3), today = LocalDate.parse("2026-10-18"))
        val client = CoffeePeekClient(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            getToken = { null }, saveToken = { error("No credential writes") },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        try { block(backend, client) } finally {
            client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close(); backend.close()
        }
    }

    @Test fun allFourTransportsAreMockOnlyAndRealServicesDecodeFixtures() = runTest { fixture { backend, client ->
        listOf(client.client, client.plainClient, client.uploadClient, client.imageClient).forEach { assertIs<MockEngine>(it.engine) }
        assertNotSame(client.client, client.plainClient)
        assertNotSame(client.client, client.imageClient)
        val shops = ShopApiService(client.client)
        assertEquals(2, shops.searchShops().getOrThrow().coffeeShops.size)
        assertEquals("Sandbox Cafe", shops.getShopDetails("sandbox-cafe").getOrThrow().name)
        assertEquals("Минск", shops.getCities().getOrThrow().single().name)
        assertEquals("cappuccino", shops.getConsumedDrinks().getOrThrow().single().slug)
        assertEquals(2, shops.getBrewMethods().getOrThrow().size)
        val roasters = RoasterApiService(client.plainClient)
        assertEquals("Sandbox Roaster", roasters.getRoasters().getOrThrow().single().name)
        assertEquals("sandbox-roaster", roasters.getRoaster("sandbox-roaster").getOrThrow().address.slug)
        assertContentEquals(byteArrayOf(1, 2, 3), client.client.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertContentEquals(byteArrayOf(1, 2, 3), client.imageClient.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertNull(backend.photo("https://example.org/media/sample"))
    } }

    @Test fun favoritesMutationsRoundTripThroughActualApiAndRemainLocal() = runTest { fixture { _, client ->
        val favorites = FavoritesApiService(client.client)
        assertEquals(3, favorites.getFavorites().getOrThrow().size)
        favorites.setFavorite("coffee_shop", "sandbox-cafe", false).getOrThrow()
        assertEquals(2, favorites.getFavorites().getOrThrow().size)
        assertFalse(ShopApiService(client.client).getShopDetails("sandbox-cafe").getOrThrow().isFavorite)
        favorites.setFavorite("coffee_shop", "sandbox-cafe", true).getOrThrow()
        assertEquals(3, favorites.getFavorites().getOrThrow().size)
    } }

    @Test fun profileChangesPersistInFakeBackendAndResetRestoresFixtures() = runTest { fixture { backend, client ->
        val users = UserApiService(client.client)
        assertEquals("sandbox@example.invalid", users.getMe().getOrThrow().email)
        users.updateUsername("Local Tester").getOrThrow()
        users.updateAbout("Local biography").getOrThrow()
        assertEquals("Local Tester", users.getMe().getOrThrow().userName)
        assertEquals("Local biography", users.getMe().getOrThrow().about)
        backend.reset(true)
        assertEquals("Sandbox", users.getMe().getOrThrow().userName)
    } }

    @Test fun redesignedProfileReadsRequiredStatsActivityFavoritesAndUnwrappedConnections() = runTest { fixture { _, client ->
        val feed = object : FeedRepository {
            override suspend fun getFeed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPage> =
                error("Profile metadata must not fetch the timeline")
        }
        val profiles = PublicProfileRepositoryImpl(client.client, feed)
        val profile = profiles.getProfile("sandbox").getOrThrow()
        assertEquals("Sandbox", profile.nickname)
        assertEquals(1, profile.stats.checkIns)
        assertFalse(profile.isFollowing)
        assertEquals(3, profiles.getFavorites("sandbox").getOrThrow().size)
        assertEquals(1, profiles.getActivity("sandbox", 2026, 10).getOrThrow().totalCheckIns)
        for (kind in PublicProfileConnectionKind.entries) {
            val connections = profiles.getConnections("sandbox", kind, 2, 10).getOrThrow()
            assertTrue(connections.items.isEmpty())
            assertEquals(2, connections.page)
            assertEquals(10, connections.pageSize)
        }
    } }

    @Test fun actualCoffeeRepositoryUsesOfflineSearchAndDetailContracts() = runTest { fixture { _, client ->
        val coffee = CoffeeRepositoryImpl(client.plainClient)
        assertEquals("Sandbox Ethiopia", coffee.search("", CoffeeFilters(), 1).getOrThrow().items.single().name)
        assertTrue(coffee.search("not-present", CoffeeFilters(), 1).getOrThrow().items.isEmpty())
        assertEquals("sandbox-ethiopia", coffee.getDetails("sandbox-ethiopia").getOrThrow().coffee.slug)
        assertTrue(coffee.getFilterGroups().getOrThrow().isEmpty())
    } }

    @Test fun nearbyDiscoveryUsesRealSearchPayloadAndSharedFavoriteFlags() = runTest { fixture { _, client ->
        val api = ShopApiService(client.client)
        val filters = ShopSearchFiltersDto(ShopSearchOriginDto(53.9, 27.56), favoritesOnly = true)
        val request = ShopSearchRequestDto(filters, page = 1, pageSize = 20)
        assertEquals("Sandbox Cafe", api.searchNearbyShops(request).getOrThrow().items.single().name)
        FavoritesApiService(client.client).setFavorite("coffee_shop", "sandbox-cafe", false).getOrThrow()
        assertTrue(api.searchNearbyShops(request).getOrThrow().items.isEmpty())
        assertEquals("Sandbox Lab", api.searchNearbyShops(request.copy(filters = filters.copy(favoritesOnly = false, isNew = true))).getOrThrow().items.single().name)
    } }

    @Test fun actualMigratedShopWriteUpdatesHistoryButNeverPublishesPrivateOrPendingRows() = runTest { fixture { _, client ->
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val feed = FeedApiService(client.client)
        assertEquals(1, feed.getFeed(20).getOrThrow().items.size)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Only local", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z",
            photos = listOf(ShopCheckInPhoto(byteArrayOf(9, 8), "sandbox.png", "image/png")))
        writer.create(input).getOrThrow()
        val visits = CheckInApiService(client.client).getMyCheckIns(1, 20).getOrThrow().items
        assertEquals(2, visits.size)
        assertEquals("Only local", visits.last().text)
        assertTrue(visits.last().photos.single().url.startsWith("/media/"))
        writer.create(input.copy(visibility = ShopCheckInVisibility.Public)).getOrThrow()
        assertEquals(1, feed.getFeed(20).getOrThrow().items.size)
    } }

    @Test fun writeErrorScenarioDoesNotChangeProfileFavoritesOrVisits() = runTest { fixture { backend, client ->
        backend.scenario = SandboxScenario.SaveError
        assertTrue(UserApiService(client.client).updateAbout("Rejected").isFailure)
        assertTrue(FavoritesApiService(client.client).setFavorite("coffee_shop", "sandbox-cafe", false).isFailure)
        backend.scenario = SandboxScenario.Ready
        assertNotEquals("Rejected", UserApiService(client.client).getMe().getOrThrow().about)
        assertEquals(3, FavoritesApiService(client.client).getFavorites().getOrThrow().size)
    } }

    @Test fun helpfulVotesDecodeRemainIdempotentAndPersistAcrossReads() = runTest { fixture { _, client ->
        val api = CheckInApiService(client.client)
        val id = api.getMyCheckIns(1, 20).getOrThrow().items.single().id
        val votes = createShopCheckInVoteRepository(client.client)
        assertEquals(CheckInHelpfulDto(true, 1), api.setHelpful(id, true).getOrThrow())
        assertEquals(ShopHelpfulVote(true, 1), votes.setHelpful(id, true).getOrThrow())
        val details = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
            .getDetails("sandbox-cafe").getOrThrow().checkIns.single()
        assertTrue(details.isHelpfulByCurrentUser)
        assertEquals(1, details.helpfulCount)
        assertEquals(ShopHelpfulVote(false, 0), votes.setHelpful(id, false).getOrThrow())
        assertEquals(CheckInHelpfulDto(false, 0), api.setHelpful(id, false).getOrThrow())
        val row = api.getMyCheckIns(1, 20).getOrThrow().items.single()
        assertFalse(row.isHelpfulByCurrentUser)
        assertEquals(0, row.helpfulCount)
    } }

    @Test fun createdCheckInsKeepTheirShopAndRejectUnknownShopSlugs() = runTest { fixture { _, client ->
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val input = ShopCheckInCreateInput("sandbox-lab", "Lab visit", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z")
        writer.create(input).getOrThrow()
        val api = CheckInApiService(client.client)
        val created = api.getMyCheckIns(1, 20).getOrThrow().items.last()
        assertEquals("sandbox-lab", created.shop?.slug)
        assertEquals("Sandbox Lab", created.shopName)
        val shops = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
        assertEquals(listOf(created.id), shops.getDetails("sandbox-lab").getOrThrow().userCheckIns.map { it.id })
        assertTrue(shops.getDetails("sandbox-cafe").getOrThrow().userCheckIns.none { it.id == created.id })
        assertTrue(writer.create(input.copy(shopSlug = "unknown-shop")).isFailure)
        assertEquals(2, api.getMyCheckIns(1, 20).getOrThrow().items.size)
    } }

    @Test fun publicShopRowsExcludePrivateAndPendingVisitsWhileHistoryRetainsThem() = runTest { fixture { _, client ->
        val api = CheckInApiService(client.client)
        val publishedId = api.getMyCheckIns(1, 20).getOrThrow().items.single().id
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Private visit", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z")
        writer.create(input).getOrThrow()
        writer.create(input.copy(text = "Pending visit", visibility = ShopCheckInVisibility.Public)).getOrThrow()
        val details = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
            .getDetails("sandbox-cafe").getOrThrow()
        assertEquals(listOf(publishedId), details.checkIns.map { it.id })
        assertEquals(1, details.overview.reviewCount)
        assertEquals(3, details.userCheckIns.size)
        assertEquals(3, api.getMyCheckIns(1, 20).getOrThrow().items.size)
        assertEquals(1, FeedApiService(client.client).getFeed(20).getOrThrow().items.size)
    } }

    @Test fun unimplementedMutationsAreRejectedAndEmptyStateIsSharedAcrossSources() = runTest { fixture { backend, client ->
        assertFalse(client.client.post("/api/ModerationShops").status.isSuccess())
        val api = CheckInApiService(client.client)
        val id = api.getMyCheckIns(1, 20).getOrThrow().items.single().id
        assertEquals(HttpStatusCode.MethodNotAllowed, client.client.post("/api/v1/check-ins/$id/helpful").status)
        assertTrue(api.report(id, "Sandbox report").isFailure)
        assertEquals(1, api.getMyCheckIns(1, 20).getOrThrow().items.size)
        assertTrue(ShopApiService(client.client).getShopDetails("unknown-shop").isFailure)
        backend.reset(false)
        assertTrue(ShopApiService(client.client).searchShops().getOrThrow().coffeeShops.isEmpty())
        assertTrue(FeedApiService(client.client).getFeed(20).getOrThrow().items.isEmpty())
        assertTrue(FavoritesApiService(client.client).getFavorites().getOrThrow().isEmpty())
    } }
}
