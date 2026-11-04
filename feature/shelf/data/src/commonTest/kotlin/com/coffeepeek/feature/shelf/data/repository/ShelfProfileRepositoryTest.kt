package com.coffeepeek.feature.shelf.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfProfileRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = MutableStateFlow<String?>("first")
    private val sessionGeneration = MutableStateFlow(0)
    private val account = object : ShelfAccount {
        override val owner get() = owners.value
        override val owners = this@ShelfProfileRepositoryTest.owners
        override val visibilityScope get() = owner?.let { "$it:${sessionGeneration.value}" }
        override val visibilityScopes = combine(owners, sessionGeneration) { user, generation -> user?.let { "$it:$generation" } }
    }
    private val clients = mutableListOf<HttpClient>()
    private val requests = mutableListOf<HttpRequestData>()
    private val bag = "11111111-1111-4111-8111-111111111111"
    private val photo = "22222222-2222-4222-8222-222222222222"
    private val photoPath get() = "/api/v1/profiles/author/shelf/bags/$bag/photos/$photo"

    @AfterTest fun closeClients() { clients.forEach { it.close() } }

    @Test fun ownerVisibilityRequiresAnExplicitServerBooleanAndPinsEveryWrite() = runTest(dispatcher) {
        val bodies = mutableListOf<Boolean>()
        val repo = repository { request ->
            assertEquals("first", request.headers["X-Test-Owner"])
            if (request.method == HttpMethod.Put) bodies += payload(request).getValue("isVisible").jsonPrimitive.boolean
            ok("""{"isVisible":${bodies.lastOrNull() ?: false}}""")
        }
        assertFalse(repo.visibility().getOrThrow())
        assertTrue(repo.setVisibility(true).getOrThrow())
        assertFalse(repo.setVisibility(false).getOrThrow())
        assertEquals(listOf(true, false), bodies)
        assertTrue(requests.all { it.url.encodedPath == "/api/v1/shelf/profile-visibility" })
    }

    @Test fun missingVisibilityDoesNotBecomeAnImplicitFalseSuccess() = runTest(dispatcher) {
        assertTrue(repository { ok("{}") }.visibility().isFailure)
    }

    @Test fun confirmedFalseAndTrueAreCachedButAFailedRefreshKeepsTheLastValue() = runTest(dispatcher) {
        var response = """{"isVisible":false}"""
        val repo = repository { ok(response) }
        assertNull(repo.cachedVisibility())
        assertFalse(repo.visibility().getOrThrow())
        assertEquals(false, repo.cachedVisibility())
        response = """{"isVisible":true}"""
        assertTrue(repo.visibility().getOrThrow())
        assertEquals(true, repo.cachedVisibility())
        response = "{}"
        assertTrue(repo.visibility().isFailure)
        assertEquals(true, repo.cachedVisibility())
    }

    @Test fun aSameUserLoginInvalidatesThePreviousPreference() = runTest(dispatcher) {
        val repo = repository { ok("""{"isVisible":true}""") }
        repo.visibility().getOrThrow()
        assertEquals(true, repo.cachedVisibility())
        sessionGeneration.value++
        assertNull(repo.cachedVisibility())
    }

    @Test fun anOlderReadCannotReplaceTheLastConfirmedWrite() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var reads = 0
        val repo = repository { request ->
            if (request.method == HttpMethod.Put) ok("""{"isVisible":false}""")
            else {
                if (++reads == 2) gate.await()
                ok("""{"isVisible":true}""")
            }
        }
        repo.visibility().getOrThrow()
        val pending = async { repo.visibility() }
        runCurrent()
        repo.setVisibility(false).getOrThrow()
        assertEquals(false, repo.cachedVisibility())
        gate.complete(Unit)
        pending.await()
        assertEquals(false, repo.cachedVisibility())
    }

    @Test fun publicProjectionAndPhotosNeverSendOwnerCredentials() = runTest(dispatcher) {
        val repo = repository { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers["X-Test-Owner"])
            if (request.url.encodedPath == photoPath) respond(byteArrayOf(1, 2, 3)) else ok(shelf(photoPath))
        }
        val shelf = repo.publicShelf("author").getOrThrow()!!
        assertEquals(1, shelf.totalCount)
        assertEquals("https://fixture.invalid$photoPath", shelf.items.single().photoUrl)
        assertContentEquals(byteArrayOf(1, 2, 3), repo.photo(shelf.items.single().photoUrl!!).getOrThrow())
    }

    @Test fun hiddenShelfIsAnExplicitNullInsteadOfAnEmptyPublishedList() = runTest(dispatcher) {
        assertNull(repository { ok("null") }.publicShelf("author").getOrThrow())
    }

    @Test fun privateAndForeignPhotoUrlsAreRejectedBeforeARequest() = runTest(dispatcher) {
        val repo = repository { error("Forbidden request") }
        assertTrue(repo.photo("https://other.invalid$photoPath").isFailure)
        assertTrue(repo.photo("https://fixture.invalid/api/v1/shelf/bags/$bag/photos/$photo").isFailure)
        assertTrue(repo.photo("https://fixture.invalid$photoPath?token=secret").isFailure)
        assertTrue(requests.isEmpty())
    }

    @Test fun projectionCannotSubstituteAnotherBagsPhotoOrDuplicateItems() = runTest(dispatcher) {
        var data = shelf(photoPath.replace(bag, photo))
        val repo = repository { ok(data) }
        assertTrue(repo.publicShelf("author").isFailure)
        val item = """{"id":"$bag","name":"Coffee"}"""
        data = """{"items":[$item,$item],"totalCount":2}"""
        assertTrue(repo.publicShelf("author").isFailure)
    }

    @Test fun accountReplacementCancelsInFlightOwnerResponse() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = repository { gate.await(); ok("""{"isVisible":true}""") }
        val read = async { repo.visibility() }
        runCurrent()
        owners.value = "second"
        runCurrent()
        assertTrue(read.isCancelled)
        gate.complete(Unit)
        assertFailsWith<CancellationException> { read.await() }
    }

    @Test fun signedOutAndInvalidSlugsCannotReachTransport() = runTest(dispatcher) {
        val repo = repository { error("Forbidden request") }
        owners.value = null
        assertTrue(repo.setVisibility(true).isFailure)
        assertTrue(repo.publicShelf("../other").isFailure)
        assertTrue(repo.publicShelf("author%2Fother").isFailure)
        assertTrue(requests.isEmpty())
    }

    @Test fun photoContentLengthAndStreamingBytesAreBothBounded() = runTest(dispatcher) {
        var declared = true
        val repo = repository {
            if (declared) respond(byteArrayOf(1), headers = headersOf(HttpHeaders.ContentLength, "8388609"))
            else respond(ByteArray(8 * 1024 * 1024 + 1))
        }
        assertTrue(repo.photo("https://fixture.invalid$photoPath").isFailure)
        declared = false
        assertTrue(repo.photo("https://fixture.invalid$photoPath").isFailure)
    }

    @Test fun successfulProjectionIsCachedIncludingAnExplicitHiddenShelf() = runTest(dispatcher) {
        var data = shelf(photoPath)
        val repo = repository { ok(data) }
        assertNull(repo.cachedPublicShelf("author"))
        val visible = repo.publicShelf("author").getOrThrow()
        assertEquals(visible, repo.cachedPublicShelf("author")?.shelf)
        data = "null"
        repo.publicShelf("author").getOrThrow()
        assertNotNull(repo.cachedPublicShelf("author"))
        assertNull(repo.cachedPublicShelf("author")?.shelf)
    }

    @Test fun accountChangeImmediatelyRejectsCachedAndPendingProjections() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var pending = false
        val repo = repository { if (pending) gate.await(); ok(shelf(photoPath)) }
        repo.publicShelf("author").getOrThrow()
        pending = true
        val read = async { repo.publicShelf("author") }
        runCurrent()
        owners.value = "second"
        assertNull(repo.cachedPublicShelf("author"))
        gate.complete(Unit)
        read.await()
        assertNull(repo.cachedPublicShelf("author"))
        owners.value = null
        assertNull(repo.cachedPublicShelf("author"))
    }

    @Test fun visibilityWriteInvalidatesTheCacheAndLateReadsCannotRestoreIt() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var reads = 0
        val repo = repository { request ->
            if (request.method == HttpMethod.Put) ok("""{"isVisible":false}""")
            else {
                val read = ++reads
                if (read == 2) gate.await()
                ok(if (read == 3) "null" else shelf(photoPath))
            }
        }
        repo.publicShelf("author").getOrThrow()
        val read = async { repo.publicShelf("author") }
        runCurrent()
        repo.setVisibility(false).getOrThrow()
        assertNull(repo.cachedPublicShelf("author"))
        repo.publicShelf("author").getOrThrow()
        gate.complete(Unit)
        read.await()
        assertNotNull(repo.cachedPublicShelf("author"))
        assertNull(repo.cachedPublicShelf("author")?.shelf)
    }

    private fun repository(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        createShelfProfileRepository(client(true, handler), client(false, handler), "https://fixture.invalid", account) { request, owner ->
            request.headers["X-Test-Owner"] = owner
        }

    private fun client(owned: Boolean, handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine) {
            engine {
                dispatcher = this@ShelfProfileRepositoryTest.dispatcher
                addHandler { request -> requests += request; handler(request) }
            }
            if (owned) defaultRequest { headers[HttpHeaders.Authorization] = "Bearer private" }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }.also(clients::add)

    private fun payload(request: HttpRequestData) =
        Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject

    private fun shelf(path: String) = """{"items":[{"id":"$bag","name":"Coffee","coffeeSlug":"coffee","photoUrl":"$path"}],"totalCount":1}"""

    private fun MockRequestHandleScope.ok(data: String) = respond("""{"isSuccess":true,"data":$data}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"))
}
