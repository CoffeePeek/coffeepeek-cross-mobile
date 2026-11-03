package com.coffeepeek.admin.sandbox.backend



import com.coffeepeek.feature.auth.domain.classifyLoginFailure

import com.coffeepeek.feature.appupdate.impl.UpdateCheckFeedback

import kotlin.test.assertNotNull
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
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.feature.favorites.data.repository.createAccountFavorites
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.appupdate.data.AppVersionResponse
import com.coffeepeek.feature.appupdate.data.toUpdate
import com.coffeepeek.feature.coffee.data.repository.createCoffeeRepository
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.profile.data.createPublicProfileData
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
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
private class LocalSession : SessionStore {
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

private fun accountFavorites(client: ApiClients) = createAccountFavorites(client.client, LocalSession())
private fun shopEntry(slug: String) =
    FavoriteEntry(FavoriteKind.CoffeeShop, FavoriteAddress(slug, "/coffee-shops/$slug", 1, false))

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
    private val friend = SandboxApplicationBackend.FRIEND_ID
    private val friendPhoto = "/api/v1/check-ins/${SandboxApplicationBackend.FRIEND_VISIT_ID}/photos/${SandboxApplicationBackend.FRIEND_PHOTO_ID}/variants/feed/1/feed.png"
    private suspend fun json(response: io.ktor.client.statement.HttpResponse) = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonObject

    private suspend fun fixture(block: suspend (SandboxBackend, ApiClients) -> Unit) {
        val backend = SandboxBackend(byteArrayOf(1, 2, 3), today = LocalDate.parse("2026-10-18"))
        val client = ApiClients(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { return null }
                override suspend fun save(tokens: SessionTokens?) { error("No credential writes") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        try { block(backend, client) } finally {
            client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close(); backend.close()
        }
    }

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
        val coffee = createCoffeeRepository(client.plainClient)
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
        val outage = assertFailsWith<com.coffeepeek.core.network.ServiceUnavailableException> { client.imageClient.get("${SandboxBackend.BASE_URL}$friendPhoto") }
        assertEquals(HttpStatusCode.InternalServerError.value, outage.statusCode)
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
            assertFalse(automatic.showPrompt); assertNull(automatic.feedback)
            val manual = check(manual = true).second
            assertTrue(manual.checkFailed); assertEquals(UpdateCheckFeedback.CheckFailed, manual.feedback)

            backend.updatePolicy = SandboxUpdatePolicy.UpToDate
            assertEquals(UpdateCheckFeedback.UpToDate, check(manual = true).second.feedback)
        } finally { scope.cancel() }
    } }
}

