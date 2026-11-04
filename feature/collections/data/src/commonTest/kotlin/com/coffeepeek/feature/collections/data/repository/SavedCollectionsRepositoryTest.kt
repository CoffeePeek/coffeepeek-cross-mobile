package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCollectionsRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val id = "12345678-1234-1234-1234-1234567890ab"
    private val coverId = "22345678-1234-1234-1234-1234567890ab"
    private val compactId get() = id.replace("-", "")
    private val body get() = """{"data":{"id":"$id","title":"Places","mode":"list","visibility":"private","revision":2,"items":[]}}"""
    private val defaultBody get() = body.replace("\"Places\"", "\"Избранное\"")
    private val defaultList get() = """{"data":[{"id":"$id","title":"Избранное","mode":"list","visibility":"private","revision":2}]}"""
    private val publicBody get() = body.replace("\"private\"", "\"public\"").replace("\"items\":[]", "\"items\":[],\"canonicalPath\":\"/collections/$compactId\"")
    private val account = Account()

    @Test fun firstOwnerReadCreatesPrivateFavoritesForRoasterMembership() = runTest(dispatcher) {
        var creates = 0
        val client = defaultRoasterClient { creates++ }
        try {
            val target = SavedCollectionTarget(CollectionPlaceKind.Roaster, "roaster-slug")
            val list = repo(client, client).mine(target).getOrThrow().single()
            assertEquals(SavedCollectionVisibility.Private, list.visibility)
            assertFalse(list.containsItem)
            assertEquals(1, creates)
        } finally { client.close() }
    }

    @Test fun simultaneousOwnerReadsCreateOnlyOneDefaultList() = runTest(dispatcher) {
        var created = false
        var creates = 0
        val client = client { request -> when (request.method) {
            HttpMethod.Get -> respond(if (created) defaultList else """{"data":[]}""")
            HttpMethod.Post -> { creates++; created = true; respond(defaultBody, HttpStatusCode.Created) }
            else -> error("Unexpected request")
        } }
        try {
            val repository = repo(client, client)
            val reads = List(2) { async { repository.mine().getOrThrow() } }
            reads.forEach { assertEquals("Избранное", it.await().single().title) }
            assertEquals(1, creates)
        } finally { client.close() }
    }

    @Test fun existingPrivateFavoritesNeedsNoCreation() = runTest(dispatcher) {
        val client = client { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(defaultList)
        }
        try {
            assertEquals("Избранное", repo(client, client).mine().getOrThrow().single().title)
        } finally { client.close() }
    }

    @Test fun publishingDefaultFavoritesDoesNotCreateAPrivateDuplicate() = runTest(dispatcher) {
        val client = client { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(defaultList.replace("\"private\"", "\"public\""))
        }
        try {
            val lists = repo(client, client).mine().getOrThrow()
            assertEquals(1, lists.size)
            assertEquals(SavedCollectionVisibility.Public, lists.single().visibility)
        } finally { client.close() }
    }

    @Test fun serverDefaultEnsureIsIdempotentAndReloadsTargetMembership() = runTest(dispatcher) {
        var reads = 0
        var ensures = 0
        val client = client(defaultEndpoint = true) { request ->
            when (request.method) {
                HttpMethod.Get -> respond(if (++reads == 1) """{"data":[]}""" else
                    defaultList.replace("\"revision\":2", "\"revision\":2,\"isDefault\":true,\"containsItem\":true"))
                HttpMethod.Put -> { ensures++; respond(defaultBody.replace("\"revision\":2", "\"revision\":2,\"isDefault\":true")) }
                else -> error("No generic collection creation")
            }
        }
        try {
            val list = repo(client, client).mine(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "shop")).getOrThrow().single()
            assertTrue(list.isDefault)
            assertTrue(list.containsItem)
            assertEquals(1, ensures)
        } finally { client.close() }
    }

    @Test fun renamedPublishedDefaultKeepsItsServerIdentity() = runTest(dispatcher) {
        val client = client(defaultEndpoint = true) { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(defaultList.replace("Избранное", "Weekend").replace("\"private\"", "\"public\"")
                .replace("\"revision\":2", "\"revision\":2,\"isDefault\":true"))
        }
        try {
            val list = repo(client, client).mine().getOrThrow().single()
            assertEquals("Weekend", list.title)
            assertTrue(list.isDefault)
        } finally { client.close() }
    }

    @Test fun onlySuccessfulMutationsInvalidateOtherCollectionScreens() = runTest(dispatcher) {
        val client = client { request ->
            respond(body, if (request.method == HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.Conflict)
        }
        try {
            val repository = repo(client, client)
            var changes = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeChanges().collect { changes++ } }
            repository.create(SavedCollectionMetadata("List")).getOrThrow()
            assertTrue(repository.update(id, SavedCollectionMetadata("Other"), 2).isFailure)
            runCurrent()
            assertEquals(1, changes)
        } finally { client.close() }
    }

    @Test fun membershipChangesSendCanonicalToggleDeltasAlongsideLegacySelection() = runTest(dispatcher) {
        val client = client { request ->
            if (request.method == HttpMethod.Get) respond(defaultList.replace("\"revision\":2", "\"revision\":2,\"containsItem\":true"))
            else {
                val sent = payload(request)
                assertTrue(sent.getValue("collectionIds").jsonArray.isEmpty())
                assertTrue(sent.getValue("addedCollectionIds").jsonArray.isEmpty())
                assertEquals(id, sent.getValue("removedCollectionIds").jsonArray.single().jsonPrimitive.content)
                respond(defaultList)
            }
        }
        try { repo(client, client).changeMemberships(SavedCollectionTarget(CollectionPlaceKind.Roaster, "roaster"),
            emptySet(), setOf(compactId)).getOrThrow() }
        finally { client.close() }
    }

    @Test fun changedAccountDuringListReadCannotCreateForTheNewOwner() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var creates = 0
        val client = client { request ->
            if (request.method == HttpMethod.Post) { creates++; respond(defaultBody, HttpStatusCode.Created) }
            else { gate.await(); respond("""{"data":[]}""") }
        }
        try {
            val repository = repo(client, client)
            val pending = async {
                try { repository.mine(); null }
                catch (cancelled: CancellationException) { cancelled }
            }
            runCurrent()
            account.owner.value = "second"
            gate.complete(Unit)
            assertIs<CancellationException>(pending.await())
            assertEquals(0, creates)
        } finally { client.close() }
    }

    @Test fun conflictDuringDefaultCreationReReadsTheWinner() = runTest(dispatcher) {
        var reads = 0
        val client = client { request -> when (request.method) {
            HttpMethod.Get -> respond(if (++reads == 1) """{"data":[]}""" else defaultList)
            HttpMethod.Post -> respond("{}", HttpStatusCode.Conflict)
            else -> error("Unexpected request")
        } }
        try {
            assertEquals("Избранное", repo(client, client).mine().getOrThrow().single().title)
            assertEquals(2, reads)
        } finally { client.close() }
    }

    @Test fun cancelledListRequestNeverCreatesTheDefault() = runTest(dispatcher) {
        var creates = 0
        val client = client { request ->
            if (request.method == HttpMethod.Post) { creates++; respond(defaultBody, HttpStatusCode.Created) }
            else throw CancellationException("Interrupted list read")
        }
        try {
            assertFailsWith<CancellationException> { repo(client, client).mine() }
            assertEquals(0, creates)
        } finally { client.close() }
    }

    @Test fun emptyListCreationPinsOwnerAndDefaultsToPrivate() = runTest(dispatcher) {
        val client = client { request ->
            assertEquals("first", request.headers["X-Test-Owner"])
            val json = payload(request)
            assertEquals("private", json.getValue("visibility").jsonPrimitive.content)
            assertTrue(json.getValue("items").jsonArray.isEmpty())
            respond(body)
        }
        try {
            val result = repo(client, client).create(SavedCollectionMetadata("Places")).getOrThrow()
            assertEquals(SavedCollectionVisibility.Private, result.summary.visibility)
            assertNull(result.summary.url)
            assertTrue(result.places.isEmpty())
        } finally { client.close() }
    }

    @Test fun publicListReadNeverLoadsAccountAndRejectsPrivatePayloads() = runTest(dispatcher) {
        val private = client { error("No private request expected") }
        var privatePayload = false
        val public = client { request ->
            assertNull(request.headers["X-Test-Owner"])
            assertNull(request.headers[HttpHeaders.Authorization])
            respond(if (privatePayload) body else publicBody)
        }
        try {
            val repository = repo(private, public)
            assertEquals("https://coffeepeek.by/collections/$compactId", repository.public(id).getOrThrow().summary.url)
            privatePayload = true
            assertTrue(repository.public(id).isFailure)
            assertEquals(0, account.loads)
        } finally { private.close(); public.close() }
    }

    @Test fun privateCoverIgnoresDtoUrlAndUsesOnlyTheOwnedFeaturePath() = runTest(dispatcher) {
        var requests = 0
        val private = client { request ->
            assertEquals("fixture.invalid", request.url.host)
            assertEquals("first", request.headers["X-Test-Owner"])
            requests++
            if (requests == 1) respond(body.replace("\"items\":[]", "\"items\":[],\"coverPhoto\":{\"id\":\"$coverId\",\"url\":\"https://foreign.invalid/steal\"}"))
            else {
                assertEquals("/api/v1/saved-collections/$compactId/cover/${coverId.replace("-", "")}", request.url.encodedPath)
                respond(byteArrayOf(1, 2, 3), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
            }
        }
        val public = client { error("Private cover must never use the anonymous client") }
        try {
            val repository = repo(private, public)
            val value = repository.owned(id).getOrThrow()
            assertContentEquals(byteArrayOf(1, 2, 3), repository.cover(id, checkNotNull(value.summary.cover)).getOrThrow())
        } finally { private.close(); public.close() }
    }

    @Test fun publicCoverIsAnonymousAndDoesNotUseTheOwnerEndpoint() = runTest(dispatcher) {
        val private = client { error("No owner request expected") }
        val public = client { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers["X-Test-Owner"])
            assertTrue(request.url.encodedPath.startsWith("/api/v1/favorite-collections/"))
            respond(byteArrayOf(1), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
        }
        try {
            assertContentEquals(byteArrayOf(1), repo(private, public).cover(id, SavedCollectionCover(coverId, false)).getOrThrow())
            assertEquals(0, account.loads)
        } finally { private.close(); public.close() }
    }

    @Test fun oversizedCoverIsRejectedBeforeReturningBytes() = runTest(dispatcher) {
        val client = client { respond(ByteArray(SavedCollectionRules.maxCoverBytes + 1)) }
        try {
            assertTrue(repo(client, client).cover(id, SavedCollectionCover(coverId, true)).isFailure)
        } finally { client.close() }
    }

    @Test fun staleRevisionMapsToConflictAndSendsExpectedRevision() = runTest(dispatcher) {
        val client = client { request ->
            assertEquals(7, payload(request).getValue("expectedRevision").jsonPrimitive.int)
            respond("{}", HttpStatusCode.Conflict)
        }
        try {
            val result = repo(client, client).update(id, SavedCollectionMetadata("Changed"), 7)
            assertIs<SavedCollectionConflict>(result.exceptionOrNull())
        } finally { client.close() }
    }

    @Test fun validatedHttpPolicyMapsConflictButPreservesOtherResponseExceptions() = runTest(dispatcher) {
        var status = HttpStatusCode.Conflict
        val client = client(validateStatus = true) { respond("{}", status) }
        try {
            val repository = repo(client, client)
            assertIs<SavedCollectionConflict>(repository.update(id, SavedCollectionMetadata("Changed"), 2).exceptionOrNull())
            status = HttpStatusCode.Forbidden
            val failure = assertIs<ClientRequestException>(repository.mine().exceptionOrNull())
            assertEquals(HttpStatusCode.Forbidden, failure.response.status)
        } finally { client.close() }
    }

    @Test fun membershipsUseOneOwnedRequestWithOnlyTheExplicitCollectionIds() = runTest(dispatcher) {
        var writes = 0
        val client = client { request ->
            writes++
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/v1/saved-collections/membership/coffee_shop/shop", request.url.encodedPath)
            assertEquals("first", request.headers["X-Test-Owner"])
            assertEquals(listOf(id), payload(request).getValue("collectionIds").jsonArray.map { it.jsonPrimitive.content })
            respond("""{"data":[]}""")
        }
        try {
            assertTrue(repo(client, client).memberships(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "shop"), setOf(compactId)).isSuccess)
            assertEquals(1, writes)
        } finally { client.close() }
    }

    @Test fun featuredSelectionUsesHyphenatedGuidInJsonBody() = runTest(dispatcher) {
        val client = client { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/v1/saved-collections/featured", request.url.encodedPath)
            assertEquals(id, payload(request).getValue("collectionId").jsonPrimitive.content)
            respond("""{"data":{"collectionId":"$id"}}""")
        }
        try {
            assertTrue(repo(client, client).setFeatured(compactId).isSuccess)
        } finally { client.close() }
    }

    @Test fun liveReservationPayloadUploadsWithoutLeakingOwnerToSignedStorage() = runTest(dispatcher) {
        val private = client { request ->
            assertEquals("first", request.headers["X-Test-Owner"])
            assertEquals("/api/Photos/collection-cover", request.url.encodedPath)
            respond("""{"data":[{"photoId":"$coverId","uploadUrl":"https://storage.fixture.invalid/upload?signature=fake","storageKey":"collection-covers/first/file"}]}""")
        }
        val public = client { request ->
            assertEquals("storage.fixture.invalid", request.url.host)
            assertEquals("/upload", request.url.encodedPath)
            assertNull(request.headers["X-Test-Owner"])
            assertNull(request.headers[HttpHeaders.Authorization])
            assertEquals("is_permanent=False", request.headers["x-amz-tagging"])
            assertEquals("image/jpeg", request.body.contentType?.toString() ?: request.headers[HttpHeaders.ContentType])
            respond("")
        }
        try {
            val result = repo(private, public).uploadCover(CollectionCoverInput(byteArrayOf(1, 2), "image/jpeg", "cover.jpg")).getOrThrow()
            assertEquals("collection-covers/first/file", result.storageKey)
            assertEquals(2L, result.size)
        } finally { private.close(); public.close() }
    }

    @Test fun accountReplacementDuringLiveReservationPreventsTheStoragePut() = runTest(dispatcher) {
        val private = client {
            account.owner.value = "second"
            respond("""{"data":[{"photoId":"$coverId","uploadUrl":"https://storage.fixture.invalid/upload","storageKey":"collection-covers/first/file"}]}""")
        }
        val public = client { error("An old-account upload must not continue") }
        try {
            assertFailsWith<CancellationException> {
                repo(private, public).uploadCover(CollectionCoverInput(byteArrayOf(1), "image/jpeg", "cover.jpg"))
            }
        } finally { private.close(); public.close() }
    }

    @Test fun obsoleteIdAndUrlAliasesCannotMasqueradeAsLiveReservationFields() = runTest(dispatcher) {
        var payload = ""
        var storageWrites = 0
        val private = client { respond(payload) }
        val public = client { storageWrites++; respond("") }
        try {
            val repository = repo(private, public)
            val aliases = listOf("\"id\":\"$coverId\",\"url\":\"https://storage.fixture.invalid/upload\"",
                "\"photoId\":\"$coverId\",\"url\":\"https://storage.fixture.invalid/upload\"",
                "\"id\":\"$coverId\",\"uploadUrl\":\"https://storage.fixture.invalid/upload\"")
            aliases.forEach { fields ->
                payload = """{"data":[{$fields,"storageKey":"collection-covers/first/file"}]}"""
                assertTrue(repository.uploadCover(CollectionCoverInput(byteArrayOf(1), "image/jpeg", "cover.jpg")).isFailure)
            }
            assertEquals(0, storageWrites)
        } finally { private.close(); public.close() }
    }

    @Test fun publicFeaturedNoneDoesNotLoadThePrivateSession() = runTest(dispatcher) {
        val private = client { error("Featured is anonymous") }
        val public = client { respond("""{"data":null}""") }
        try {
            assertNull(repo(private, public).featured("author").getOrThrow())
            assertEquals(0, account.loads)
        } finally { private.close(); public.close() }
    }

    @Test fun coverDeadlineIncludesWaitingBehindFourOtherRequests() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        val client = client {
            requests++
            gate.await()
            respond(byteArrayOf(1))
        }
        try {
            val repository = repo(client, client)
            val jobs = List(5) { async { repository.cover(id, SavedCollectionCover(coverId, false)) } }
            runCurrent()
            assertEquals(4, requests)
            advanceTimeBy(8_000)
            runCurrent()
            assertTrue(jobs.all { it.isCancelled })
            assertEquals(4, requests)
        } finally { client.close() }
    }

    @Test fun queuedPrivateCoverRechecksOwnerBeforeSendingAnyRequest() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        val client = client {
            requests++
            gate.await()
            respond(byteArrayOf(1))
        }
        try {
            val repository = repo(client, client)
            val jobs = List(5) { async { repository.cover(id, SavedCollectionCover(coverId, true)) } }
            runCurrent()
            assertEquals(4, requests)
            account.owner.value = "second"
            gate.complete(Unit)
            runCurrent()
            assertTrue(jobs.all { it.isCancelled })
            assertEquals(4, requests)
        } finally { client.close() }
    }

    private fun repo(private: HttpClient, public: HttpClient) = createSavedCollectionsRepository(private, public,
        "https://fixture.invalid", account) { builder, owner -> builder.headers["X-Test-Owner"] = owner }

    private fun defaultRoasterClient(onCreate: () -> Unit) = client { request ->
        assertEquals("first", request.headers["X-Test-Owner"])
        when (request.method) {
            HttpMethod.Get -> {
                assertEquals("roaster", request.url.parameters["kind"])
                assertEquals("roaster-slug", request.url.parameters["slug"])
                respond("""{"data":[]}""")
            }
            HttpMethod.Post -> {
                onCreate()
                assertEquals("private", payload(request).getValue("visibility").jsonPrimitive.content)
                assertEquals("Избранное", payload(request).getValue("title").jsonPrimitive.content)
                respond(defaultBody, HttpStatusCode.Created)
            }
            else -> error("Unexpected request")
        }
    }

    private fun payload(request: io.ktor.client.request.HttpRequestData): JsonObject =
        Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject

    private fun client(validateStatus: Boolean = false,
        defaultEndpoint: Boolean = false,
        handler: suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        HttpClient(MockEngine) {
            expectSuccess = validateStatus
            engine {
                dispatcher = this@SavedCollectionsRepositoryTest.dispatcher
                addHandler { request ->
                    if (!defaultEndpoint && request.url.encodedPath.endsWith("/saved-collections/default"))
                        respond("{}", HttpStatusCode.NotFound)
                    else handler(request)
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun MockRequestHandleScope.respond(value: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(value, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private class Account : CollectionsAccount {
        val owner = MutableStateFlow<String?>("first")
        var loads = 0
        override val identities = owner
        override fun currentId() = owner.value
        override suspend fun loadId(): String? { loads++; return owner.value }
    }
}
