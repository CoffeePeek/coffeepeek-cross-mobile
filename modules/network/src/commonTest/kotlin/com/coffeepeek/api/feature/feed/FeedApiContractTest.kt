package com.coffeepeek.api.feature.feed

import com.coffeepeek.api.configureSessionAuthentication
import com.coffeepeek.api.readCurrentSessionForRequests
import com.coffeepeek.api.model.response.AuthResp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class FeedApiContractTest {
    private val headers = headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.CacheControl to listOf("no-store"))
    private val feed = """{"data":{"items":[{"publishedAtUtc":"2026-10-07T10:00:00Z","checkIn":{"id":"visit","shop":null,"author":null,"text":"Кофе","rating":{"coffee":5,"service":4,"place":4},"isHelpfulByCurrentUser":true}}],"nextCursor":"opaque_-cursor"},"isSuccess":true,"statusCode":null}"""
    private val empty = """{"data":{"items":[],"nextCursor":null},"isSuccess":true,"statusCode":null}"""

    @Test
    fun optionalTokenIsSentOnFirstPublicRequestAndTracksLoginLogoutAndAccountSwitches() = runBlocking {
        var token: AuthResp? = null
        val sent = mutableListOf<String?>()
        val client = client(MockEngine { request ->
            sent += request.headers[HttpHeaders.Authorization]
            assertEquals("/api/v1/feed", request.url.encodedPath)
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            respond(empty, headers = headers)
        }, { token }, { token = it })
        try {
            val api = FeedApiService(client)
            assertTrue(api.getFeed(20).getOrThrow().items.isEmpty())
            token = AuthResp("first")
            api.getFeed(20).getOrThrow()
            token = AuthResp("second")
            api.getFeed(20).getOrThrow()
            token = null
            api.getFeed(20).getOrThrow()
            assertEquals(listOf(null, "Bearer first", "Bearer second", null), sent)
        } finally { client.close() }
    }

    @Test
    fun filtersAndOpaqueCursorAreQueryParametersAndPageSizeCanChange() = runBlocking {
        var requests = 0
        val client = client(MockEngine { request ->
            assertEquals("city", request.url.parameters["citySlug"])
            assertEquals("shop-alias", request.url.parameters["coffeeShopSlug"])
            assertEquals("author", request.url.parameters["authorSlug"])
            if (requests++ == 0) {
                assertEquals("20", request.url.parameters["pageSize"])
                assertNull(request.url.parameters["cursor"])
                respond(feed, headers = headers)
            } else {
                assertEquals("100", request.url.parameters["pageSize"])
                assertEquals("opaque_-cursor", request.url.parameters["cursor"])
                respond(empty, headers = headers)
            }
        })
        try {
            val api = FeedApiService(client)
            val first = api.getFeed(20, citySlug = "city", coffeeShopSlug = "shop-alias", authorSlug = "author").getOrThrow()
            assertEquals("2026-10-07T10:00:00Z", first.items.single().publishedAtUtc)
            assertTrue(first.items.single().checkIn.isHelpfulByCurrentUser)
            assertNull(first.items.single().checkIn.shop)
            val last = api.getFeed(100, first.nextCursor, "city", "shop-alias", "author").getOrThrow()
            assertNull(last.nextCursor)
            assertTrue(last.items.isEmpty())
        } finally { client.close() }
    }

    @Test
    fun httpErrorsAndUnsuccessfulOrIncompleteEnvelopesRemainFailures() = runBlocking {
        for ((status, body) in listOf(
            HttpStatusCode.BadRequest to "",
            HttpStatusCode.NotFound to "",
            HttpStatusCode.Unauthorized to "",
            HttpStatusCode.OK to """{"isSuccess":false,"data":{"items":[],"nextCursor":null}}""",
            HttpStatusCode.OK to """{"isSuccess":true,"data":null}""",
        )) {
            val client = client(MockEngine { respond(body, status, headers) })
            try {
                val error = FeedApiService(client).getFeed(20, "stale").exceptionOrNull()
                assertNotNull(error)
                if (status == HttpStatusCode.BadRequest) assertTrue((error as FeedApiException).restartPagination)
            } finally { client.close() }
        }
    }

    @Test
    fun invalidPageSizesAndEmptyOrOversizedSlugsDoNotRequestABroaderFeed() = runBlocking {
        var requests = 0
        val client = client(MockEngine { requests++; respond(empty, headers = headers) })
        try {
            val api = FeedApiService(client)
            for (size in listOf(0, -1, 101)) assertTrue(api.getFeed(size).isFailure)
            assertTrue(api.getFeed(20, citySlug = "").isFailure)
            assertTrue(api.getFeed(20, authorSlug = "a".repeat(101)).isFailure)
            assertEquals(0, requests)
        } finally { client.close() }
    }

    @Test
    fun expiredTokenRefreshesAndSubsequentFeedUsesNewTokenImmediately() = runBlocking {
        var token: AuthResp? = AuthResp("expired", "refresh")
        var refreshes = 0
        val sent = mutableListOf<String?>()
        val client = client(MockEngine { request ->
            sent += request.headers[HttpHeaders.Authorization]
            if (request.headers[HttpHeaders.Authorization] == "Bearer expired")
                respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            else respond(empty, headers = headers)
        }, { token }, { token = it }, { value ->
            assertEquals("refresh", value); refreshes++; AuthResp("fresh", "next-refresh")
        })
        try {
            val api = FeedApiService(client)
            api.getFeed(20).getOrThrow(); api.getFeed(20).getOrThrow()
            assertEquals<List<String?>>(listOf("Bearer expired", "Bearer fresh", "Bearer fresh"), sent)
            assertEquals(1, refreshes)
        } finally { client.close() }
    }

    @Test
    fun refreshFinishingAfterLogoutCannotRestorePreviousSession() = runBlocking {
        var token: AuthResp? = AuthResp("expired", "refresh")
        var requests = 0
        val client = client(MockEngine {
            requests++
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        }, { token }, { token = it }, {
            token = null
            AuthResp("old-account-refreshed", "refresh")
        })
        try {
            assertTrue(FeedApiService(client).getFeed(20).isFailure)
            assertNull(token)
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test
    fun optionalAuthenticationDoesNotSendTokenToExternalPhotoHostEvenAfterChallenge() = runBlocking {
        val client = client(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        }, { AuthResp("private-token") })
        try { assertEquals(HttpStatusCode.Unauthorized, client.get("https://photos.example/image").status) }
        finally { client.close() }
    }

    private fun client(engine: MockEngine, token: () -> AuthResp? = { null }, save: (AuthResp?) -> Unit = {},
        refresh: suspend (String) -> AuthResp = { error("Unexpected refresh") }) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("https://api.example") }
        configureSessionAuthentication("https://api.example", token, save, refresh)
    }.also { it.readCurrentSessionForRequests() }
}
