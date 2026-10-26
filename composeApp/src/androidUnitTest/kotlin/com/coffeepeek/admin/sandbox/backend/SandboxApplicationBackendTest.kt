package com.coffeepeek.admin.sandbox.backend

import kotlin.test.assertNotNull
import com.coffeepeek.api.service.AuthService
import com.coffeepeek.admin.feature.passwordrecovery.data.PasswordRecoveryRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.feature.appupdate.impl.UpdateUiState
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment
import com.coffeepeek.admin.di.appupdate.SettingsDismissedUpdateStore
import com.coffeepeek.feature.appupdate.data.AppUpdateRepositoryImpl
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.api.feature.favorites.FavoritesApiService
import com.coffeepeek.api.feature.feed.FeedApiService
import com.coffeepeek.api.service.*
import com.coffeepeek.api.model.request.*
import com.coffeepeek.api.model.request.ShopSearchRequestDto
import com.coffeepeek.api.model.request.ShopSearchFiltersDto
import com.coffeepeek.api.model.request.ShopSearchOriginDto
import com.coffeepeek.feature.appupdate.data.AppVersionResponse
import com.coffeepeek.feature.appupdate.data.toUpdate
import com.coffeepeek.admin.feature.coffee.data.CoffeeRepositoryImpl
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.feature.profile.data.createPublicProfileData
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.admin.di.authorblocking.SessionAuthorBlockingAccount
import com.coffeepeek.feature.authorblocking.impl.createAuthorBlockingFeature
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

    @Test fun migratedOwnProfileReadsAndEditsShareOneCacheInTheLocalBackend() = runTest { fixture { _, client ->
        val data = com.coffeepeek.feature.profile.data.repository.createProfileAccountData(client.client, client.uploadClient, LocalSession(), backgroundScope)
        assertEquals("Sandbox", data.accounts.loadOwnProfile(false).getOrThrow().userName)
        data.edit.updateUsername("Local Tester").getOrThrow()
        data.edit.updateAbout("Local biography").getOrThrow()
        assertEquals("Local Tester", data.accounts.cachedOwnProfile()?.userName)
        assertEquals("Local biography", data.accounts.loadOwnProfile(false).getOrThrow().about)
        assertEquals("sandbox", data.accounts.cachedOwnProfile()?.address?.slug)
        assertNull(data.deletion.existingRequest().getOrThrow())
    } }

    @Test fun migratedPublicFavoriteEnrichmentUsesNewShopAndRoasterReaders() = runTest { fixture { _, client ->
        val store = object : com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore {
            private var saved: String? = null
            override suspend fun read() = saved
            override suspend fun write(value: String) { saved = value }
        }
        val roasters = com.coffeepeek.feature.roaster.data.repository.createRoasterRepository(client.client, SandboxBackend.BASE_URL, store) { 0L }
        val details = com.coffeepeek.feature.profile.data.repository.createPublicProfileFavoriteDetails(
            com.coffeepeek.feature.shop.data.repository.createPublicShopDetails(client.client),
            com.coffeepeek.feature.roaster.data.repository.createPublicRoasterDetails(roasters))
        val favorites = createPublicProfileData(client.client, details).getFavorites("sandbox").getOrThrow()
        assertEquals(3, favorites.size)
        assertTrue(favorites.first { it.kind == com.coffeepeek.feature.profile.domain.PublicFavoriteKind.CoffeeShop }.description?.isNotBlank() == true)
        assertTrue(favorites.first { it.kind == com.coffeepeek.feature.profile.domain.PublicFavoriteKind.Roaster }.description?.isNotBlank() == true)
    } }

    @Test fun publicProfileCheckInsUseMigratedReadersAndRemainPublicForOwnAndGuestProfiles() = runTest { fixture { _, client ->
        val community = com.coffeepeek.feature.community.data.repository.createCommunityRepository(client.client, "https://sandbox.coffeepeek.invalid")
        val posts = com.coffeepeek.feature.community.data.repository.createPublicAuthorPosts(community)
        val reader = com.coffeepeek.feature.profile.data.repository.createPublicProfileCheckIns(posts)
        val own = reader.page("sandbox").getOrThrow()
        assertEquals(1, own.items.size)
        assertEquals("Sandbox", own.items.single().authorName)
        val friend = reader.page("sandbox-friend").getOrThrow()
        assertEquals(1, friend.items.size)
        assertTrue(reader.page("not-found").getOrThrow().items.isEmpty())
    } }

    @Test fun migratedContentReportUsesOnlyTheLocalSandboxContract() = runTest { fixture { backend, client ->
        val repository = com.coffeepeek.feature.contentreport.data.repository.createContentReportRepository(client.client)
        val target = com.coffeepeek.feature.contentreport.domain.model.ContentReportTarget(
            com.coffeepeek.feature.contentreport.domain.model.ContentReportKind.CheckIn, SandboxApplicationBackend.FRIEND_VISIT_ID)
        repository.submit(target, "  Тестовая причина  ").getOrThrow()
        assertEquals(1, backend.contentReportCount)
        assertIs<MockEngine>(client.client.engine)
        backend.reset(populated = true)
        assertEquals(0, backend.contentReportCount)
    } }

    @Test fun locallyCreatedOwnCheckInCannotBeReportedEvenWithoutAuthorId() = runTest { fixture { backend, client ->
        val created = client.client.post("/api/v1/check-ins") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("coffeeShopSlug", "sandbox-cafe"); put("text", "Локальный чекин") })
        }.body<JsonObject>()["data"]!!.jsonObject
        assertNull(created["authorId"])
        val repository = com.coffeepeek.feature.contentreport.data.repository.createContentReportRepository(client.client)
        val target = com.coffeepeek.feature.contentreport.domain.model.ContentReportTarget(
            com.coffeepeek.feature.contentreport.domain.model.ContentReportKind.CheckIn, created["id"]!!.jsonPrimitive.content)
        val failure = repository.submit(target, "Тестовая причина").exceptionOrNull()
        assertEquals(com.coffeepeek.feature.contentreport.domain.model.ContentReportProblem.Forbidden,
            assertIs<com.coffeepeek.feature.contentreport.domain.model.ContentReportFailure>(failure).problem)
        assertEquals(0, backend.contentReportCount)
    } }
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
        assertEquals(listOf("Sandbox Roaster", "Sandbox Light Lab", "Sandbox Decaf"), roasters.getRoasters().getOrThrow().map { it.name })
        assertEquals("sandbox-roaster", roasters.getRoaster("sandbox-roaster").getOrThrow().address.slug)
        assertContentEquals(byteArrayOf(1, 2, 3), client.client.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertContentEquals(byteArrayOf(1, 2, 3), client.imageClient.get("${SandboxBackend.BASE_URL}/media/sample").body<ByteArray>())
        assertNull(backend.photo("https://example.org/media/sample"))
    } }

    @Test fun actualRoasterRepositoryReadsTagsShopsContactsAndPartialRoastersFromFixtures() = runTest { fixture { backend, client ->
        val store = object : com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore {
            var saved: String? = null
            override suspend fun read() = saved
            override suspend fun write(value: String) { saved = value }
        }
        val roasters = com.coffeepeek.feature.roaster.data.repository.createRoasterRepository(client.client, SandboxBackend.BASE_URL, store) { 1_000L }
        val list = roasters.getRoasters().getOrThrow()
        assertEquals(listOf("sandbox-roaster", "sandbox-light", "sandbox-decaf"), list.map { it.slug })
        assertEquals(listOf("light", "decaf"), list.first().tags.map { it.slug })
        assertEquals(listOf(2, 1, 0), list.map { it.coffeeShopsCount })
        assertNotNull(list.first().photoUrl); assertNull(list.last().photoUrl)
        assertNotNull(store.saved)
        val full = roasters.getRoaster("sandbox-roaster").getOrThrow()
        assertEquals("@sandbox.roaster", full.contact?.instagramLink)
        assertEquals("sandbox-roaster.example/about", full.contact?.siteLink)
        assertEquals("Минск, Тестовая улица, 5", full.location?.address)
        assertEquals(2, full.photos.size)
        assertEquals(listOf("sandbox-cafe" to true, "sandbox-lab" to false), full.shops.map { it.slug to (it.photoUrl != null) })
        val partial = roasters.getRoaster("sandbox-light").getOrThrow()
        assertNull(partial.contact?.instagramLink); assertEquals("https://light.sandbox.invalid", partial.contact?.siteLink)
        assertNull(partial.location); assertTrue(partial.photos.isEmpty())
        val bare = roasters.getRoaster("sandbox-decaf").getOrThrow()
        assertNull(bare.about); assertNull(bare.contact); assertTrue(bare.shops.isEmpty())
        assertTrue(roasters.getRoaster("missing").isFailure)
        backend.reset(false)
        assertTrue(roasters.getRoaster("sandbox-roaster").isFailure)
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
        val profiles = createPublicProfileData(client.client)
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

    @Test fun actualCoffeeRepositoryUsesOfflineSearchFiltersOffersAndDetailContracts() = runTest { fixture { _, client ->
        val coffee = CoffeeRepositoryImpl(client.plainClient)
        assertEquals(listOf("Sandbox Ethiopia", "Sandbox Kenya", "Sandbox Brazil"), coffee.search("", CoffeeFilters(), 1).getOrThrow().items.map { it.name })
        assertEquals(listOf("Sandbox Kenya"), coffee.search("ken", CoffeeFilters(), 1).getOrThrow().items.map { it.name })
        assertTrue(coffee.search("not-present", CoffeeFilters(), 1).getOrThrow().items.isEmpty())
        val fruity = coffee.search("", CoffeeFilters(mapOf("taste" to setOf("fruity"))), 1).getOrThrow().items.map { it.slug }
        assertEquals(listOf("sandbox-ethiopia", "sandbox-kenya"), fruity)
        assertEquals(listOf("sandbox-brazil"), coffee.search("", CoffeeFilters(mapOf("roast" to setOf("medium"))), 1).getOrThrow().items.map { it.slug })
        assertEquals(listOf("sandbox-ethiopia", "sandbox-brazil"), coffee.search("", CoffeeFilters(availableOnly = true), 1).getOrThrow().items.map { it.slug })
        val details = coffee.getDetails("sandbox-ethiopia").getOrThrow()
        assertEquals("sandbox-ethiopia", details.coffee.slug)
        assertEquals(2, details.coffee.offers.size)
        assertTrue(details.coffee.offers.all { it.sourceUrl != null })
        assertEquals(setOf("taste", "roast"), coffee.getFilterGroups().getOrThrow().map { it.code }.toSet())
        assertTrue(coffee.getDetails("missing").isFailure)
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

    @Test fun helpfulVotesDecodeRemainIdempotentAndPersistAcrossReads() = runTest { fixture { _, client ->
        val api = CheckInApiService(client.client)
        val id = api.getMyCheckIns(1, 20).getOrThrow().items.single().id
        val votes = createShopCheckInVoteRepository(client.client)
        assertEquals(CheckInHelpfulDto(true, 1), api.setHelpful(id, true).getOrThrow())
        assertEquals(ShopHelpfulVote(true, 1), votes.setHelpful(id, true).getOrThrow())
        val details = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
            .getDetails("sandbox-cafe").getOrThrow().checkIns.first { it.id == id }
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
        val shops = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
        val published = shops.getDetails("sandbox-cafe").getOrThrow().checkIns.map { it.id }
        val writer = createShopCheckInRepository(client.client, client.uploadClient)
        val input = ShopCheckInCreateInput("sandbox-cafe", "Private visit", ShopRating(4, 5, 4), "2026-10-18T09:00:00Z")
        writer.create(input).getOrThrow()
        writer.create(input.copy(text = "Pending visit", visibility = ShopCheckInVisibility.Public)).getOrThrow()
        val details = shops.getDetails("sandbox-cafe").getOrThrow()
        assertEquals(published, details.checkIns.map { it.id })
        assertEquals(published.size, details.overview.reviewCount)
        assertEquals(3, details.userCheckIns.size)
        assertEquals(3, api.getMyCheckIns(1, 20).getOrThrow().items.size)
        assertEquals(published.size, FeedApiService(client.client).getFeed(20).getOrThrow().items.size)
    } }

    @Test fun anotherAuthorsVotesPreserveExistingCountAndBlockVisibility() = runTest { fixture { _, client ->
        val id = SandboxApplicationBackend.FRIEND_VISIT_ID
        val votes = createShopCheckInVoteRepository(client.client)
        assertEquals(ShopHelpfulVote(true, 3), votes.setHelpful(id, true).getOrThrow())
        assertEquals(ShopHelpfulVote(true, 3), votes.setHelpful(id, true).getOrThrow())
        val shops = createShopDetailsRepository(client.client, SandboxBackend.BASE_URL) { 0 }
        val row = shops.getDetails("sandbox-cafe").getOrThrow().checkIns.first { it.id == id }
        assertTrue(row.isHelpfulByCurrentUser)
        assertEquals(3, row.helpfulCount)
        assertEquals(ShopHelpfulVote(false, 2), votes.setHelpful(id, false).getOrThrow())
        client.client.put("/api/v1/me/blocks/$friend")
        assertTrue(votes.setHelpful(id, true).isFailure)
        client.client.delete("/api/v1/me/blocks/$friend")
        val restored = shops.getDetails("sandbox-cafe").getOrThrow().checkIns.first { it.id == id }
        assertFalse(restored.isHelpfulByCurrentUser)
        assertEquals(2, restored.helpfulCount)
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
        val feature = createAuthorBlockingFeature(client.client, SessionAuthorBlockingAccount(LocalSession()))
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

    @Test fun profileEditingFollowAndDeletionFixturesFollowTheRealContracts() = runTest { fixture { backend, client ->
        val users = UserApiService(client.client)
        // Edit: validation and a taken name fail without changing the profile; valid writes persist.
        assertTrue(users.updateUsername("A").isFailure)
        assertTrue(users.updateUsername(SandboxApplicationBackend.FRIEND_NAME).isFailure)
        assertTrue(users.updateAbout("x".repeat(601)).isFailure)
        assertEquals("Sandbox", users.getMe().getOrThrow().userName)
        assertTrue(users.updateUsername("Renamed").isSuccess)
        assertTrue(users.updateAbout("New about").isSuccess)
        assertEquals("Renamed", users.getMe().getOrThrow().userName)
        assertEquals("New about", users.getMe().getOrThrow().about)

        // Deletion: no request until DELETE; the request then exists with a resend window; reset clears it.
        assertNull(users.getAccountDeletionRequest().getOrThrow())
        val created = users.requestAccountDeletion().getOrThrow()
        assertEquals("Pending", created.status)
        assertEquals(created.requestId, users.getAccountDeletionRequest().getOrThrow()?.requestId)
        assertTrue(java.time.Instant.parse(created.resendAvailableAtUtc).isAfter(java.time.Instant.now()))
        backend.reset(populated = true)
        assertNull(users.getAccountDeletionRequest().getOrThrow())

        // Follow: friend only, bare body, counters follow the state; self 400; unknown slug 404 for follow and profile.
        val profiles = createPublicProfileData(client.client)
        val friend = SandboxApplicationBackend.FRIEND_SLUG
        assertEquals(false, profiles.getProfile(friend).getOrThrow().isFollowing)
        assertEquals(true, profiles.setFollowing(friend, true).getOrThrow().isFollowing)
        assertEquals(1, profiles.getProfile(friend).getOrThrow().stats.followers)
        assertEquals(false, profiles.setFollowing(friend, false).getOrThrow().isFollowing)
        assertTrue(profiles.setFollowing("sandbox", true).isFailure)
        assertTrue(profiles.setFollowing("nobody", true).isFailure)
        assertTrue(profiles.getProfile("nobody").isFailure)

        // Writes fail closed under the error scenarios and leave the state unchanged.
        backend.scenario = SandboxScenario.SaveError
        assertTrue(profiles.setFollowing(friend, true).isFailure)
        assertTrue(users.requestAccountDeletion().isFailure)
        backend.scenario = SandboxScenario.Ready
        assertEquals(false, profiles.getProfile(friend).getOrThrow().isFollowing)
        assertNull(users.getAccountDeletionRequest().getOrThrow())
    } }
    @Test fun updatePromptStateFollowsTheFixturePoliciesThroughTheRealRepository() = runBlocking { fixture { backend, client ->
        class Settings : SettingRepository {
            val rows = mutableMapOf<String, String>()
            override suspend fun save(model: Setting) { rows[model.key] = model.value }
            override suspend fun read(key: String) = rows[key]?.let { Setting(key, it) }
            override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
            override suspend fun readAll() = rows.map { Setting(it.key, it.value) }
            override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
            override suspend fun delete(key: String) { rows.remove(key) }
        }
        val settings = Settings()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        // A new state per call stands for a fresh process: only the dismissed version survives.
        suspend fun check(manual: Boolean = false, state: AppUpdateController = AppUpdateController(AppUpdateRepositoryImpl(client.client, AppConfigUpdateEnvironment, SettingsDismissedUpdateStore(settings)), object : AppUpdateEnvironment { override val versionCode: Long? = 5; override val platform = "android"; override val channel: String? = "play" }, scope)): Pair<AppUpdateController, UpdateUiState> {
            state.check(manual); return state to state.state.first { !it.checking }
        }
        try {
            assertFalse(check().second.showPrompt)

            backend.updatePolicy = SandboxUpdatePolicy.Available
            val (optional, shown) = check()
            assertTrue(shown.showPrompt); assertFalse(shown.update!!.isRequired(5))
            optional.dismiss()
            assertFalse(check(state = optional).second.showPrompt)
            assertFalse(check().second.showPrompt)
            assertTrue(check(manual = true).second.showPrompt)

            backend.updatePolicy = SandboxUpdatePolicy.Required
            val (required, mandatory) = check()
            assertTrue(mandatory.showPrompt); assertTrue(mandatory.update!!.isRequired(5))
            required.dismiss()
            assertTrue(check().second.showPrompt)

            backend.updatePolicy = SandboxUpdatePolicy.Failure
            val automatic = check().second
            assertFalse(automatic.showPrompt); assertNull(automatic.message)
            val manual = check(manual = true).second
            assertTrue(manual.checkFailed); assertEquals("Не удалось проверить обновления. Попробуйте позже.", manual.message)

            backend.updatePolicy = SandboxUpdatePolicy.UpToDate
            assertEquals("Установлена актуальная версия", check(manual = true).second.message)
        } finally { scope.cancel() }
    } }
    @Test fun accountFixturesAnswerTheRealAuthServiceAndRecoveryRepository() = runTest { fixture { backend, client ->
        val auth = AuthService(client.client, client.plainClient)
        val recovery = PasswordRecoveryRepositoryImpl(client.plainClient)

        // Default: sign-in succeeds with a token whose subject is the Sandbox account; nothing is taken; OAuth stays blocked.
        val login = auth.login(" Tester@Example.com ", "any-password").getOrThrow()
        assertEquals("11111111-1111-4111-8111-111111111111", com.coffeepeek.core.session.JwtUtils.extractUserId(login.accessToken))
        assertEquals("sandbox-refresh-token", login.refreshToken)
        assertEquals(false, auth.isEmailTaken("new@example.com").getOrThrow())
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)
        assertTrue(recovery.requestReset("tester@example.com").isSuccess)
        assertTrue(auth.logout("sandbox-refresh-token").isSuccess)
        assertTrue(auth.googleLogin("token").isFailure)
        assertTrue(auth.beginAppleLogin().isFailure)

        backend.authPolicy = SandboxAuthPolicy.WrongPassword
        val rejected = auth.login("tester@example.com", "wrong").exceptionOrNull()
        assertNotNull(rejected)
        assertEquals("Неверный email или пароль", com.coffeepeek.admin.utils.localizedLoginError(rejected.message))
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)

        backend.authPolicy = SandboxAuthPolicy.EmailTaken
        assertEquals(true, auth.isEmailTaken("taken@example.com").getOrThrow())
        assertTrue(auth.login("tester@example.com", "password1").isSuccess)

        backend.authPolicy = SandboxAuthPolicy.RegistrationFailure
        assertTrue(auth.register("Tester", "new@example.com", "password1").isFailure)
        assertEquals(false, auth.isEmailTaken("new@example.com").getOrThrow())

        backend.authPolicy = SandboxAuthPolicy.RecoveryFailure
        assertTrue(recovery.requestReset("tester@example.com").isFailure)
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)

        // The scenario chips still win over the account policy, so offline and 5xx also reach sign-in.
        backend.authPolicy = SandboxAuthPolicy.Works
        backend.scenario = SandboxScenario.Offline
        assertTrue(auth.login("tester@example.com", "password1").isFailure)
        backend.scenario = SandboxScenario.ServerError
        assertTrue(auth.login("tester@example.com", "password1").isFailure)
        backend.scenario = SandboxScenario.Ready
        backend.reset(true)
        assertEquals(SandboxAuthPolicy.Works, backend.authPolicy)
    } }
    @Test fun feedFixturesPageThroughTheRealFeedServiceAndHonourBlocksAndFailures() = runTest { fixture { backend, client ->
        val feed = FeedApiService(client.client)
        // Default: the visible fixtures in one page.
        assertEquals(2, feed.getFeed(20).getOrThrow().items.size)
        assertNull(feed.getFeed(20).getOrThrow().nextCursor)

        backend.feed = SandboxFeed.Paged
        val first = feed.getFeed(20).getOrThrow()
        assertEquals(20, first.items.size); assertEquals("20", first.nextCursor)
        val second = feed.getFeed(20, first.nextCursor).getOrThrow()
        val third = feed.getFeed(20, second.nextCursor).getOrThrow()
        val ids = (first.items + second.items + third.items).map { it.checkIn.id }
        assertEquals(47, ids.size); assertEquals(ids.size, ids.toSet().size); assertNull(third.nextCursor)
        assertTrue((first.items + second.items + third.items).any { it.checkIn.kind == "Home" && it.checkIn.home?.beanName != null })
        // A blocked author disappears from every page and returns on unblock.
        client.client.put("/api/v1/me/blocks/$friend")
        val visible = feed.getFeed(100).getOrThrow().items
        assertEquals(47 - 1 - 15, visible.size); assertTrue(visible.none { it.checkIn.authorId == friend })
        client.client.delete("/api/v1/me/blocks/$friend")
        assertEquals(47, feed.getFeed(100).getOrThrow().items.size)
        // Votes work for generated posts too and keep their state.
        val vote = json(client.client.put("/api/v1/check-ins/sandbox-feed-1/helpful"))
        assertTrue(vote["isHelpful"]!!.jsonPrimitive.boolean); assertEquals(2, vote["helpfulCount"]!!.jsonPrimitive.int)
        assertEquals(1, json(client.client.delete("/api/v1/check-ins/sandbox-feed-1/helpful"))["helpfulCount"]!!.jsonPrimitive.int)

        backend.feed = SandboxFeed.PageFailsOnce
        val firstPage = feed.getFeed(20).getOrThrow()
        assertTrue(feed.getFeed(20, firstPage.nextCursor).isFailure)
        assertEquals(20, feed.getFeed(20, firstPage.nextCursor).getOrThrow().items.size)

        backend.feed = SandboxFeed.InvalidCursor
        assertEquals(20, feed.getFeed(20).getOrThrow().items.size)
        val restart = feed.getFeed(20, "20").exceptionOrNull()
        assertTrue((restart as com.coffeepeek.api.feature.feed.FeedApiException).restartPagination)

        backend.feed = SandboxFeed.Empty
        assertTrue(feed.getFeed(20).getOrThrow().items.isEmpty())
        backend.feed = SandboxFeed.Failure
        assertTrue(feed.getFeed(20).isFailure)

        backend.feed = SandboxFeed.Paged
        backend.reset(true)
        assertEquals(SandboxFeed.Single, backend.feed)
    } }
    @Test fun settingsFixturesServeTheUpdatePolicyAndTheCityCatalogThroughTheRealParsers() = runTest { fixture { backend, client ->
        // The shared client throws on a non-2xx answer; the app's update state treats that exactly like "no policy".
        suspend fun update() = runCatching {
            client.client.get("/api/v1/app-version") { parameter("platform", "android") }.body<AppVersionResponse>().toUpdate()
        }.getOrNull()
        // Default: nothing newer than any real build, nothing required.
        assertEquals(1L, update()?.versionCode); assertFalse(update()!!.isAvailable(current = 5))
        backend.updatePolicy = SandboxUpdatePolicy.Available
        assertTrue(update()!!.isAvailable(current = 5)); assertFalse(update()!!.isRequired(current = 5))
        backend.updatePolicy = SandboxUpdatePolicy.Required
        assertTrue(update()!!.isRequired(current = 5))
        assertEquals("sandbox.coffeepeek.invalid", io.ktor.http.Url(update()!!.url).host)
        backend.updatePolicy = SandboxUpdatePolicy.Failure
        assertNull(update())

        val shops = ShopApiService(client.client)
        assertEquals(listOf("Минск"), shops.getCities().getOrThrow().map { it.name })
        backend.cities = SandboxCities.Several
        assertEquals(listOf("Минск", "Брест", "Гродно"), shops.getCities().getOrThrow().map { it.name })
        backend.cities = SandboxCities.Empty
        assertTrue(shops.getCities().getOrThrow().isEmpty())
        backend.cities = SandboxCities.Failure
        assertTrue(shops.getCities().isFailure)

        // reset() returns both fixtures to their defaults; the scenarios still win over them.
        backend.reset(populated = true)
        assertEquals(listOf("Минск"), shops.getCities().getOrThrow().map { it.name })
        assertEquals(1L, update()?.versionCode)
        backend.updatePolicy = SandboxUpdatePolicy.Available
        backend.scenario = SandboxScenario.ServerError
        assertNull(update()); assertTrue(shops.getCities().isFailure)
    } }

    @Test fun contributionFixturesServeTheModerationListsAndAcceptSubmissionsThroughTheRealServices() = runTest { fixture { backend, client ->
        val shops = ShopApiService(client.client)
        val roasters = RoasterApiService(client.client)
        val changes = ShopChangeRequestApiService(client.client)

        // Seeded: a few submissions per status; an approved shop opens the catalog only when it is published.
        val approved = shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).getOrThrow()
        assertEquals(listOf("shop-approved-published", "shop-approved-waiting"), approved.moderationShops.map { it.id })
        assertEquals(listOf("sandbox-cafe", null), approved.moderationShops.map { it.publishedShop?.slug })
        assertEquals(1, shops.getMyModerationShops(ModerationStatusDto.Pending, 1, 20).getOrThrow().totalItems)
        val rejected = shops.getMyModerationShops(ModerationStatusDto.Rejected, 1, 20).getOrThrow()
        assertEquals(SandboxContributionsBackend.REJECTED_REASON, rejected.moderationShops.single().rejectedReason)
        assertEquals(1, roasters.getMyModerationRoasters(ModerationStatusDto.Approved, 1, 20).getOrThrow().items.size)
        assertEquals("Недостаточно данных", roasters.getMyModerationRoasters(ModerationStatusDto.Rejected, 1, 20).getOrThrow().items.single().rejectedReason)
        assertEquals(listOf("change-approved"), changes.getMine(1, 20, ModerationStatusDto.Approved).getOrThrow().items.map { it.id })
        assertEquals(listOf("change-rejected"), changes.getMine(1, 20, ModerationStatusDto.Rejected).getOrThrow().items.map { it.id })
        assertEquals(listOf("change-pending"), changes.getMine(1, 20, ModerationStatusDto.Pending, "sandbox-cafe", ShopChangeSectionDto.Contacts).getOrThrow().items.map { it.id })
        assertTrue(changes.getMine(1, 20, ModerationStatusDto.Pending, "sandbox-lab").getOrThrow().items.isEmpty())

        // A submitted shop appears as pending; invalid input is rejected with the server message and stores nothing.
        shops.createShop(CreateShopReq(name = "Новая кофейня", address = "Улица, 5", cityId = "minsk")).getOrThrow()
        assertEquals(2, shops.getMyModerationShops(ModerationStatusDto.Pending, 1, 20).getOrThrow().totalItems)
        val invalid = shops.createShop(CreateShopReq(name = " ", address = "Улица, 5", cityId = "minsk"))
        assertEquals("Укажите название, адрес и город", invalid.exceptionOrNull()?.message)
        assertEquals(2, shops.getMyModerationShops(ModerationStatusDto.Pending, 1, 20).getOrThrow().totalItems)

        // Roaster: the server message and the address check come back; a city without an address is refused.
        val submitted = roasters.submitRoaster(CreateRoasterSubmissionReq(name = "Новый обжарщик", cityId = "minsk", address = "Улица, 1")).getOrThrow()
        assertEquals("Pending", submitted.data.status); assertTrue(submitted.data.isAddressValidated)
        assertEquals("Обжарщик отправлен на модерацию", submitted.message)
        assertTrue(roasters.submitRoaster(CreateRoasterSubmissionReq(name = "Без адреса", cityId = "minsk")).isFailure)
        assertEquals(2, roasters.getMyModerationRoasters(ModerationStatusDto.Pending, 1, 20).getOrThrow().totalItems)

        // Changes: create, read, edit (stays pending), unknown shop, approved request cannot be edited.
        val created = changes.create(CreateShopChangeRequestBody("sandbox-lab", ShopChangeSectionDto.Description, ShopChangePayloadDto(description = "Новое"))).getOrThrow()
        assertEquals(ModerationStatusDto.Pending, created.status)
        assertEquals("Новое", changes.getById(created.id).getOrThrow().payload.description)
        val edited = changes.update(created.id, UpdateShopChangeRequestBody(ShopChangeSectionDto.Tags, ShopChangePayloadDto(tagIds = listOf("wifi")))).getOrThrow()
        assertEquals(ShopChangeSectionDto.Tags, edited.section); assertEquals(created.createdAtUtc, edited.createdAtUtc)
        assertTrue(changes.create(CreateShopChangeRequestBody("unknown", ShopChangeSectionDto.Tags, ShopChangePayloadDto())).isFailure)
        assertTrue(changes.update("change-approved", UpdateShopChangeRequestBody(ShopChangeSectionDto.Tags, ShopChangePayloadDto())).isFailure)
        assertTrue(changes.getById("missing").isFailure)

        // The wizard and the editor read the equipment, bean and tag catalogs.
        assertEquals(2, shops.getEquipment().getOrThrow().size)
        assertEquals(2, shops.getBeans().getOrThrow().size)
        assertEquals(2, shops.getShopTags().getOrThrow().size)

        // Paging: 45 extra approved shops are three pages of 20 with the remainder last.
        backend.contributions = SandboxContributions.Paged
        val first = shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).getOrThrow()
        assertEquals(47, first.totalItems); assertEquals(3, first.totalPages)
        assertEquals(7, shops.getMyModerationShops(ModerationStatusDto.Approved, 3, 20).getOrThrow().moderationShops.size)

        backend.contributions = SandboxContributions.Empty
        assertTrue(shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).getOrThrow().moderationShops.isEmpty())
        assertTrue(changes.getMine(1, 20, ModerationStatusDto.Approved).getOrThrow().items.isEmpty())

        backend.contributions = SandboxContributions.ListFailure
        assertTrue(shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).isFailure)
        assertTrue(roasters.getMyModerationRoasters(ModerationStatusDto.Approved, 1, 20).isFailure)
        assertTrue(changes.getMine(1, 20).isFailure)

        backend.contributions = SandboxContributions.SubmitFailure
        assertEquals(SandboxContributionsBackend.SUBMIT_FAILURE, shops.createShop(CreateShopReq(name = "A", address = "B", cityId = "minsk")).exceptionOrNull()?.message)
        assertTrue(roasters.submitRoaster(CreateRoasterSubmissionReq(name = "A")).isFailure)
        assertTrue(changes.create(CreateShopChangeRequestBody("sandbox-cafe", ShopChangeSectionDto.Tags, ShopChangePayloadDto())).isFailure)
        assertTrue(shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).isSuccess)

        backend.contributions = SandboxContributions.Paged
        backend.reset(true)
        assertEquals(SandboxContributions.Seeded, backend.contributions)
        assertEquals(2, shops.getMyModerationShops(ModerationStatusDto.Approved, 1, 20).getOrThrow().totalItems)
    } }

    @Test fun discoveryClustersUnfoldIntoTheSameShopsAtTheDetailZoom() = runTest { fixture { backend, client ->
        backend.discovery = SandboxDiscovery.Paged
        val shops = ShopApiService(client.client)
        val grouped = shops.getShopsInBounds(53.8, 27.4, 54.0, 27.7, zoom = 10, cityId = "minsk").getOrThrow()
        val detailed = shops.getShopsInBounds(53.8, 27.4, 54.0, 27.7, zoom = 22, cityId = "minsk").getOrThrow()
        assertTrue(grouped.clusters.isNotEmpty() || grouped.zones.isNotEmpty())
        assertEquals(detailed.shops.size, grouped.shops.size + grouped.clusters.sumOf { it.count } + grouped.zones.sumOf { it.shopCount })
        assertTrue(detailed.clusters.isEmpty())
        assertTrue(grouped.clusters.all { it.bounds.minLatitude <= it.latitude && it.latitude <= it.bounds.maxLatitude })
    } }

    @Test fun discoveryFixturesPageFilterSortAndFailThroughTheRealSearchAndMapServices() = runTest { fixture { backend, client ->
        val shops = ShopApiService(client.client)
        val minsk = listOf(53.8, 27.4, 54.0, 27.7)
        val brest = listOf(51.9, 23.5, 52.3, 24.0)
        suspend fun map(box: List<Double>, city: String? = null) = shops.getShopsInBounds(box[0], box[1], box[2], box[3], zoom = 22, cityId = city).getOrThrow()
        fun nearby(radiusKm: Int, pageSize: Int = 100, page: Int = 1, favoritesOnly: Boolean = false) = ShopSearchRequestDto(
            ShopSearchFiltersDto(ShopSearchOriginDto(53.9006, 27.5590), radiusKm = radiusKm, favoritesOnly = favoritesOnly), page = page, pageSize = pageSize,
        )

        // The default fixture is the two core shops and the one-marker map.
        assertEquals(2, shops.searchShops().getOrThrow().coffeeShops.size)

        backend.discovery = SandboxDiscovery.Paged
        val first = shops.searchShops(page = 1, pageSize = 20).getOrThrow()
        assertEquals(47, first.totalItems); assertEquals(3, first.totalPages); assertEquals(20, first.coffeeShops.size)
        assertEquals(7, shops.searchShops(page = 3, pageSize = 20).getOrThrow().coffeeShops.size)
        assertEquals(9, shops.searchShops(cityId = "brest").getOrThrow().totalItems)
        assertEquals(11, shops.searchShops(query = "Discovery Shop 1").getOrThrow().totalItems)
        assertEquals(15, shops.searchShops(roasterIds = listOf("sandbox-decaf")).getOrThrow().totalItems)
        assertEquals(4, shops.searchShops(minRating = 4.9).getOrThrow().totalItems)
        assertEquals(16, shops.searchShops(type = "Specialty").getOrThrow().totalItems)
        assertEquals(0, shops.searchShops(cityId = "brest", roasterIds = listOf("sandbox-decaf"), tagIds = listOf("none")).getOrThrow().totalItems)

        // The nearby search is sorted by distance, limited to the radius, paged, and follows the shared favorites.
        val near = shops.searchNearbyShops(nearby(radiusKm = 3)).getOrThrow()
        val distances = near.items.mapNotNull { it.distanceMeters }
        assertEquals(near.items.size, distances.size); assertEquals(distances.sorted(), distances); assertTrue(distances.all { it <= 3_000.0 })
        assertTrue(shops.searchNearbyShops(nearby(radiusKm = 1)).getOrThrow().totalItems < near.totalItems)
        val tenPerPage = shops.searchNearbyShops(nearby(radiusKm = 3, pageSize = 10)).getOrThrow()
        assertEquals((near.totalItems + 9) / 10, tenPerPage.totalPages); assertEquals(minOf(10, near.totalItems), tenPerPage.items.size)
        assertEquals("Sandbox Cafe", shops.searchNearbyShops(nearby(radiusKm = 3, favoritesOnly = true)).getOrThrow().items.single().name)
        FavoritesApiService(client.client).setFavorite("coffee_shop", "discovery-3", true).getOrThrow()
        assertEquals(2, shops.searchNearbyShops(nearby(radiusKm = 3, favoritesOnly = true)).getOrThrow().items.size)

        // The map: shops by bounds and filters, zones inside the bounds, no clusters, nothing truncated.
        val inMinsk = map(minsk)
        assertEquals(38, inMinsk.shops.size); assertTrue(inMinsk.zones.isEmpty()); assertTrue(inMinsk.clusters.isEmpty()); assertFalse(inMinsk.isTruncated)
        assertEquals(9, map(brest).shops.size); assertTrue(map(brest).zones.isEmpty())
        assertTrue(map(minsk, city = "brest").shops.isEmpty())

        backend.discovery = SandboxDiscovery.Truncated
        assertEquals(SandboxDiscoveryBackend.MAP_LIMIT, map(minsk).shops.size); assertTrue(map(minsk).isTruncated)
        assertFalse(map(brest).isTruncated)

        backend.discovery = SandboxDiscovery.Empty
        assertEquals(0, shops.searchShops().getOrThrow().totalItems)
        assertTrue(shops.searchNearbyShops(nearby(radiusKm = 3)).getOrThrow().items.isEmpty())
        assertTrue(map(minsk).shops.isEmpty()); assertTrue(map(minsk).zones.isEmpty())

        backend.discovery = SandboxDiscovery.SearchFailure
        assertTrue(shops.searchShops().isFailure); assertTrue(shops.searchNearbyShops(nearby(radiusKm = 3)).isFailure)
        assertTrue(shops.getShopsInBounds(minsk[0], minsk[1], minsk[2], minsk[3], zoom = 22).isSuccess)

        backend.discovery = SandboxDiscovery.MapFailure
        assertTrue(shops.getShopsInBounds(minsk[0], minsk[1], minsk[2], minsk[3], zoom = 22).isFailure)
        assertEquals(47, shops.searchShops().getOrThrow().totalItems)

        backend.discovery = SandboxDiscovery.Paged
        backend.reset(true)
        assertEquals(SandboxDiscovery.Seeded, backend.discovery)
        assertEquals(2, shops.searchShops().getOrThrow().coffeeShops.size)
    } }
}

