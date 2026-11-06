package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiClientsTest {
    private class Credentials(var tokens: SessionTokens?) : SessionCredentials {
        override suspend fun load() = tokens?.let { SessionTokens(it.accessToken, it.refreshToken) }
        override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens }
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
            if (this.tokens != expected) return false
            this.tokens = tokens
            return true
        }
    }

    private val requests = mutableListOf<HttpRequestData>()
    private var refreshes = 0
    private var refreshGate: CompletableDeferred<Unit>? = null
    private var refreshOver: HttpClient? = null

    private fun clients(
        credentials: Credentials = Credentials(SessionTokens("old", "refresh")),
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData = { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            else respond("ok")
        },
    ): ApiClients {
        val engine = MockEngine { request -> requests += request; handler(request) }
        val factory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = { config -> HttpClient(engine) { config() } }
        return ApiClients(
            url = "https://api.example/", cacheFolderPath = "unused", debug = false, credentials = credentials,
            refreshWith = { plain ->
                refreshOver = plain
                { refreshToken ->
                    refreshes++
                    refreshGate?.await()
                    plain.put("/api/Tokens")
                    Result.success(SessionTokens("new", refreshToken))
                }
            },
            apiClientFactory = factory, uploadClientFactory = factory,
        )
    }

    private fun ApiClients.close() { client.close(); imageClient.close(); plainClient.close(); uploadClient.close() }

    @Test fun theBaseUrlIsNormalizedAndAUrlWithoutTheSlashWorks() {
        assertEquals("https://api.coffeepeek.by", normalizeBaseUrl("https://api.coffeepeek.by/"))
        assertEquals("https://api.coffeepeek.by", normalizeBaseUrl("  https://api.coffeepeek.by///  "))
    }

    @Test fun thePlainClientCarriesTheDeviceAgentAndNoCredentials() = runBlocking {
        val clients = clients()
        try {
            clients.plainClient.get("/api/Catalogs/cities")
            val request = requests.single()
            assertEquals("https://api.example/api/Catalogs/cities", request.url.toString())
            assertNull(request.headers[HttpHeaders.Authorization])
            assertTrue(request.headers[HttpHeaders.UserAgent].orEmpty().startsWith("CoffeePeek ("))
        } finally { clients.close() }
    }

    @Test fun serverErrorsFailAndClientErrorsStayOrdinaryResponsesOnEveryTransport() = runBlocking {
        val clients = clients(handler = { request ->
            when (request.url.encodedPath) {
                "/server" -> respond("<html>boom</html>", HttpStatusCode.BadGateway)
                else -> respond("missing", HttpStatusCode.NotFound)
            }
        })
        try {
            for (transport in listOf(clients.client, clients.imageClient, clients.plainClient, clients.uploadClient)) {
                assertFailsWith<ServiceUnavailableException> { transport.get("https://api.example/server") }
                assertEquals(HttpStatusCode.NotFound, transport.get("https://api.example/other").status)
            }
        } finally { clients.close() }
    }

    @Test fun theApiAndImageClientsSendTheCurrentCredentialsAndShareOneRotation() = runBlocking {
        val gate = CompletableDeferred<Unit>().also { refreshGate = it }
        val clients = clients()
        try {
            val api = async { clients.client.get("https://api.example/api/v1/feed").status }
            val image = async { clients.imageClient.get("https://api.example/api/v1/check-ins/visit/photos/photo").status }
            while (refreshes == 0) kotlinx.coroutines.yield()
            gate.complete(Unit)
            assertEquals(HttpStatusCode.OK, api.await())
            assertEquals(HttpStatusCode.OK, image.await())
            assertEquals(1, refreshes)
            assertTrue(refreshOver === clients.plainClient, "The exchange runs over the client that carries no credentials")
            val refreshCalls = requests.filter { it.method == HttpMethod.Put }
            assertTrue(refreshCalls.all { it.headers[HttpHeaders.Authorization] == null })
        } finally { clients.close() }
    }

    @Test fun everyRequestReadsTheCurrentSessionAfterALoginOrALogout() = runBlocking {
        val credentials = Credentials(SessionTokens("first", "refresh"))
        val clients = clients(credentials) { respond("ok") }
        try {
            clients.client.get("https://api.example/a"); clients.imageClient.get("https://api.example/a")
            credentials.tokens = SessionTokens("second", "refresh")
            clients.client.get("https://api.example/b"); clients.imageClient.get("https://api.example/b")
            credentials.tokens = null
            clients.client.get("https://api.example/c"); clients.imageClient.get("https://api.example/c")
            assertEquals(
                listOf("Bearer first", "Bearer first", "Bearer second", "Bearer second", null, null),
                requests.map { it.headers[HttpHeaders.Authorization] },
            )
        } finally { clients.close() }
    }

    @Test fun theUploadClientHasNoBaseUrlAndNoCredentials() = runBlocking {
        val clients = clients { respond("ok") }
        try {
            clients.uploadClient.get("https://storage.example/object?signature=abc")
            val request = requests.single()
            assertEquals("storage.example", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
        } finally { clients.close() }
    }

    @Test fun theObserverSeesAnApiRequestOnceWithItsAuthRetry() = runBlocking {
        val clients = clients()
        val seen = mutableListOf<Triple<String, Int?, Boolean>>()
        clients.requestObserver = RequestObserver { category, _ ->
            RequestObservation { status, _, _, retried -> seen += Triple(category, status, retried) }
        }
        try {
            assertEquals(HttpStatusCode.OK, clients.client.get("https://api.example/api/shops/cafe").status)
            assertEquals(listOf<Triple<String, Int?, Boolean>>(Triple("shops", 200, true)), seen)
        } finally { clients.close() }
    }

    @Test fun aRejectedRefreshSignsTheSessionOutAndTheFailureReachesTheCaller() = runBlocking {
        val credentials = Credentials(SessionTokens("old", "refresh"))
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer")) }
        val factory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = { config -> HttpClient(engine) { config() } }
        val clients = ApiClients("https://api.example", "unused", false, credentials,
            refreshWith = { { _: String -> Result.failure<SessionTokens>(RefreshRejectedException()) } }, apiClientFactory = factory, uploadClientFactory = factory)
        try {
            assertEquals(HttpStatusCode.Unauthorized, clients.client.get("https://api.example/api/private").status)
            assertNull(credentials.tokens)
        } finally { clients.close() }
    }

    @Test fun anObserverMayBeSetAfterTheClientsAreBuilt() {
        val clients = clients()
        try { assertNotNull(clients.client); clients.requestObserver = null } finally { clients.close() }
    }
}
