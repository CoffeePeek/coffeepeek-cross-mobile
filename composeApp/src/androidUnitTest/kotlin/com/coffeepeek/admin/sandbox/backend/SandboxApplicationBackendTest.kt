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
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.createAuthorBlockingFeature
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import kotlin.test.*

/** Same shape as the Sandbox app's no-credential session, without its Android-only source set. */
private class LocalSession : SessionRepository {
    private val current = MutableStateFlow<Session?>(Session("local", "local", "11111111-1111-4111-8111-111111111111"))
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = peekSession()
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun replaceSession(expected: Session?, session: Session?) = current.compareAndSet(expected, session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = current.asStateFlow()
    override suspend fun isLoggedIn() = current.value != null
}

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
        assertEquals(2, feed.getFeed(20).getOrThrow().items.size)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Only local", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z",
            photos = listOf(ShopCheckInPhoto(byteArrayOf(9, 8), "sandbox.png", "image/png")))
        writer.create(input).getOrThrow()
        val visits = CheckInApiService(client.client).getMyCheckIns(1, 20).getOrThrow().items
        assertEquals(2, visits.size)
        assertEquals("Only local", visits.last().text)
        assertTrue(visits.last().photos.single().url.startsWith("/media/"))
        writer.create(input.copy(visibility = ShopCheckInVisibility.Public)).getOrThrow()
        assertEquals(2, feed.getFeed(20).getOrThrow().items.size)
    } }

    @Test fun writeErrorScenarioDoesNotChangeProfileFavoritesOrVisits() = runTest { fixture { backend, client ->
        backend.scenario = SandboxScenario.SaveError
        assertTrue(UserApiService(client.client).updateAbout("Rejected").isFailure)
        assertTrue(FavoritesApiService(client.client).setFavorite("coffee_shop", "sandbox-cafe", false).isFailure)
        backend.scenario = SandboxScenario.Ready
        assertNotEquals("Rejected", UserApiService(client.client).getMe().getOrThrow().about)
        assertEquals(3, FavoritesApiService(client.client).getFavorites().getOrThrow().size)
    } }

    @Test fun unimplementedMutationsAreRejectedAndEmptyStateIsSharedAcrossSources() = runTest { fixture { backend, client ->
        assertFalse(client.client.post("/api/ModerationShops").status.isSuccess())
        backend.reset(false)
        assertTrue(ShopApiService(client.client).searchShops().getOrThrow().coffeeShops.isEmpty())
        assertTrue(FeedApiService(client.client).getFeed(20).getOrThrow().items.isEmpty())
        assertTrue(FavoritesApiService(client.client).getFavorites().getOrThrow().isEmpty())
    } }

    private val friend = SandboxApplicationBackend.FRIEND_ID
    private val friendPhoto = "/api/v1/check-ins/${SandboxApplicationBackend.FRIEND_VISIT_ID}/photos/${SandboxApplicationBackend.FRIEND_PHOTO_ID}/variants/feed/1/feed.png"
    private suspend fun json(response: io.ktor.client.statement.HttpResponse) = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonObject

    @Test fun blockContractIsIdempotentPagedAndRejectsSelfAndUnknownAuthors() = runTest { fixture { _, client ->
        assertTrue(json(client.client.get("/api/v1/me/blocks") { parameter("pageSize", 100) })["items"]!!.jsonArray.isEmpty())
        repeat(2) { // idempotent
            val state = json(client.client.put("/api/v1/me/blocks/$friend"))
            assertEquals(friend, state["authorId"]!!.jsonPrimitive.content); assertTrue(state["isBlocked"]!!.jsonPrimitive.boolean)
        }
        val page = json(client.client.get("/api/v1/me/blocks") { parameter("pageSize", 1) })
        assertEquals(friend, page["items"]!!.jsonArray.single().jsonObject["authorId"]!!.jsonPrimitive.content)
        assertTrue(page["nextCursor"] is JsonNull)
        assertEquals(HttpStatusCode.BadRequest, client.client.put("/api/v1/me/blocks/${SandboxApplicationBackend.SELF_ID}").status)
        assertEquals(HttpStatusCode.NotFound, client.client.put("/api/v1/me/blocks/unknown").status)
        repeat(2) { assertFalse(json(client.client.delete("/api/v1/me/blocks/$friend"))["isBlocked"]!!.jsonPrimitive.boolean) }
        assertTrue(json(client.client.get("/api/v1/me/blocks") { parameter("pageSize", 100) })["items"]!!.jsonArray.isEmpty())
    } }

    @Test fun blockingHidesVisitsFromFeedAndShopButKeepsServerAggregatesAndUnblockRestores() = runTest { fixture { _, client ->
        val feed = FeedApiService(client.client)
        suspend fun cards() = json(client.client.get("/api/coffeeshops/sandbox-cafe"))["checkIns"]!!.jsonArray.size
        assertEquals(2, feed.getFeed(20).getOrThrow().items.size); assertEquals(2, cards())
        val count = json(client.client.get("/api/coffeeshops/sandbox-cafe"))["checkInCount"]
        client.client.put("/api/v1/me/blocks/$friend")
        assertEquals(1, feed.getFeed(20).getOrThrow().items.size); assertEquals(1, cards())
        assertEquals(count, json(client.client.get("/api/coffeeshops/sandbox-cafe"))["checkInCount"])
        assertEquals(1, json(client.client.get("/api/coffeeshops/sandbox-cafe"))["userCheckIns"]!!.jsonArray.size)
        assertTrue(json(client.client.get("/api/Users/${SandboxApplicationBackend.FRIEND_SLUG}"))["isBlockedByViewer"]!!.jsonPrimitive.boolean)
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(2, feed.getFeed(20).getOrThrow().items.size); assertEquals(2, cards())
    } }

    @Test fun slugResolvesToOpaqueAuthorIdForLegacyContent() = runTest { fixture { _, client ->
        val profile = json(client.client.get("/api/Users/${SandboxApplicationBackend.FRIEND_SLUG}"))
        assertEquals(friend, profile["authorId"]!!.jsonPrimitive.content)
        assertFalse(profile["isBlockedByViewer"]!!.jsonPrimitive.boolean)
    } }

    @Test fun protectedPhotoRevalidatesAndDisappearsWhileAuthorIsBlocked() = runTest { fixture { backend, client ->
        val first = client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto")
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals("image/png", first.headers[HttpHeaders.ContentType])
        val etag = assertNotNull(first.headers[HttpHeaders.ETag])
        assertEquals(HttpStatusCode.NotModified, client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto") {
            expectSuccess = false; header(HttpHeaders.IfNoneMatch, etag) }.status)
        client.client.put("/api/v1/me/blocks/$friend")
        // Even a valid validator must not reveal a blocked author's media.
        assertEquals(HttpStatusCode.NotFound, client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto") {
            expectSuccess = false; header(HttpHeaders.IfNoneMatch, etag) }.status)
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(HttpStatusCode.OK, client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto") { expectSuccess = false }.status)
        assertEquals(HttpStatusCode.NotFound, client.imageClient.get("${SandboxBackend.BASE_URL}/api/v1/check-ins/missing/photos/x") { expectSuccess = false }.status)
        backend.scenario = SandboxScenario.ServerError
        // The shared transport maps 5xx to its own service error instead of returning bytes.
        assertFailsWith<com.coffeepeek.api.utils.ApiException> { client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto") }
    } }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun actualAuthorBlockingFeatureBlocksThroughFakeTransportAndFailureLeavesVisibilityUnchanged() = runTest { fixture { backend, client ->
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val feature = createAuthorBlockingFeature(client.client, LocalSession())
        try {
            val author = AuthorReference(friend, SandboxApplicationBackend.FRIEND_SLUG, SandboxApplicationBackend.FRIEND_NAME)
            withContext(Dispatchers.Default) { withTimeout(10_000) {
                feature.visibility.visibility.first { it.ready }
                backend.scenario = SandboxScenario.SaveError
                feature.presenter.requestBlock(author); feature.presenter.confirm()
                feature.presenter.prompt.first { it?.error != null }
                assertTrue(feature.visibility.visibility.value.blockedIds.isEmpty())
                feature.presenter.dismiss()
                backend.scenario = SandboxScenario.Ready
                feature.presenter.requestBlock(author); feature.presenter.confirm()
                feature.visibility.visibility.first { friend in it.blockedIds }
                feature.presenter.blocks.first { state -> state.items.any { it.id == friend } }
            } }
        } finally { feature.close(); Dispatchers.resetMain() }
    } }
}
