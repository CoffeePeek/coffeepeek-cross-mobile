package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.SessionTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.client.request.HttpResponseData
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TokenRefreshTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClientFactory(MockEngine { request -> requests += request; handler(request) }).api("https://api.example")

    private fun MockRequestHandleScope.tokens(refresh: String, cookie: String? = null) = respond(
        """{"isSuccess":true,"data":{"accessToken":"new-access","refreshToken":"$refresh"}}""",
        HttpStatusCode.OK,
        if (cookie == null) json else headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.SetCookie to listOf("refreshToken=$cookie; Path=/; HttpOnly")),
    )

    @Test fun sendsOnlyTheRefreshCookieToTheTokensEndpointAndReadsTheNewPair() = runBlocking {
        val client = client { tokens("new-refresh") }
        try {
            val result = createTokenRefresh(client).refresh("old-refresh").getOrThrow()
            assertEquals(SessionTokens("new-access", "new-refresh"), result)
            val request = requests.single()
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/Tokens", request.url.encodedPath)
            assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("refreshToken=old-refresh"))
            assertTrue(request.body is OutgoingContent.NoContent)
        } finally { client.close() }
    }

    @Test fun theBodyTokenWinsThenThePercentDecodedCookieThenTheOneThatWasSent() = runBlocking {
        val withBody = client { tokens("body-refresh", cookie = "cookie-refresh") }
        val withCookie = client { tokens("", cookie = "cookie%2Frefresh") }
        val withNeither = client { tokens("") }
        try {
            assertEquals("body-refresh", createTokenRefresh(withBody).refresh("sent").getOrThrow().refreshToken)
            assertEquals("cookie/refresh", createTokenRefresh(withCookie).refresh("sent").getOrThrow().refreshToken)
            assertEquals("sent", createTokenRefresh(withNeither).refresh("sent").getOrThrow().refreshToken)
        } finally { withBody.close(); withCookie.close(); withNeither.close() }
    }

    @Test fun aDefinitiveRejectionIsRefreshRejectedAndATemporaryFailureIsNot() = runBlocking {
        for (status in listOf(400, 401, 403)) {
            val client = client { respond("", HttpStatusCode.fromValue(status)) }
            try {
                assertIs<RefreshRejectedException>(createTokenRefresh(client).refresh("r").exceptionOrNull(), "HTTP $status")
            } finally { client.close() }
        }
        for (status in listOf(500, 502, 503)) {
            val client = client { respond("", HttpStatusCode.fromValue(status)) }
            try {
                val failure = createTokenRefresh(client).refresh("r").exceptionOrNull()
                assertNotNull(failure)
                assertTrue(failure !is RefreshRejectedException, "HTTP $status must not sign the user out")
            } finally { client.close() }
        }
    }

    @Test fun aRejectedOrEmptyEnvelopeIsAFailureWithTheServersMessage() = runBlocking {
        val rejected = client { respond("""{"isSuccess":false,"message":"Token revoked","data":null}""", HttpStatusCode.OK, json) }
        val empty = client { respond("""{"isSuccess":true,"data":null}""", HttpStatusCode.OK, json) }
        try {
            assertEquals("Token revoked", createTokenRefresh(rejected).refresh("r").exceptionOrNull()?.message)
            assertTrue(createTokenRefresh(empty).refresh("r").isFailure)
        } finally { rejected.close(); empty.close() }
    }

    @Test fun aBlankTokenFailsWithoutAnyRequest() = runBlocking {
        val client = client { tokens("x") }
        try {
            assertTrue(createTokenRefresh(client).refresh(" ").isFailure)
            assertTrue(requests.isEmpty())
        } finally { client.close() }
    }

    @Test fun cancellationIsNotAFailure(): Unit = runBlocking {
        val client = client { throw CancellationException("cancelled") }
        try {
            assertFailsWith<CancellationException> { createTokenRefresh(client).refresh("r") }
        } finally { client.close() }
    }
}
