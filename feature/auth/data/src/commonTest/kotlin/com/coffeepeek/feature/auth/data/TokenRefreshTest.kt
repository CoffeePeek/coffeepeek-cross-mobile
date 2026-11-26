package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.core.network.RefreshRejectedException
import com.coffeepeek.core.network.SessionTokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import kotlin.test.*

class TokenRefreshTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val accessExpiry = Instant.parse("2030-01-02T03:04:05Z")
    private val refreshExpiry = Instant.parse("2030-02-02T03:04:05Z")
    private val payload = """{"accessToken":"new-access","refreshToken":"new-refresh","accessTokenExpiresAt":"$accessExpiry","refreshTokenExpiresAt":"$refreshExpiry"}"""

    private fun client(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClientFactory(MockEngine { request -> requests += request; handler(request) }).api("https://api.example") {
            install(HttpTimeout)
        }

    private fun MockRequestHandleScope.tokens(data: String = payload) = respond(
        """{"isSuccess":true,"data":$data}""", HttpStatusCode.OK,
        headersOf(HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.SetCookie to listOf("refreshToken=cookie-fallback; Path=/; HttpOnly")),
    )

    @Test fun sendsNativeJsonWithoutCookiesAndRetainsBothServerExpiries() = runBlocking {
        val client = client { tokens() }
        try {
            val result = createTokenRefresh(client).refresh("old-refresh").getOrThrow()
            assertEquals(SessionTokens("new-access", "new-refresh", accessExpiry, refreshExpiry), result)
            val request = requests.single()
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/tokens/native", request.url.encodedPath)
            assertNull(request.headers[HttpHeaders.Cookie])
            assertNull(request.headers[HttpHeaders.Authorization])
            assertEquals("""{"refreshToken":"old-refresh"}""", request.body.toByteArray().decodeToString())
        } finally { client.close() }
    }

    @Test fun rotatedRefreshTokenIsSentOnTheNextExchange() = runBlocking {
        val client = client { tokens(payload.replace("new-refresh", "refresh-${requests.size}")) }
        try {
            val refresh = createTokenRefresh(client)
            val first = refresh.refresh("old-refresh").getOrThrow()
            val second = refresh.refresh(assertNotNull(first.refreshToken)).getOrThrow()
            assertEquals(SessionTokens("new-access", "refresh-2", accessExpiry, refreshExpiry), second)
            assertEquals(
                listOf("""{"refreshToken":"old-refresh"}""", """{"refreshToken":"refresh-1"}"""),
                requests.map { it.body.toByteArray().decodeToString() },
            )
        } finally { client.close() }
    }

    @Test fun missingOrInvalidBodyCredentialsNeverUseCookieOrSentToken() = runBlocking {
        val fields = listOf("accessToken", "refreshToken", "accessTokenExpiresAt", "refreshTokenExpiresAt")
        for (field in fields) {
            val absent = payload.replace(Regex("\"$field\":\"[^\"]*\",?"), "").replace(",}", "}")
            val blank = payload.replace(Regex("\"$field\":\"[^\"]*\""), "\"$field\":\"\"")
            assertMalformed(absent)
            assertMalformed(blank)
        }
        assertMalformed(payload.replace(accessExpiry.toString(), "not-an-expiry"))
        assertMalformed(payload.replace(refreshExpiry.toString(), "2030-02-02"))
        assertMalformed(payload.replace("new-refresh", "sent-token"))
    }

    private suspend fun assertMalformed(data: String) {
        val client = client { tokens(data) }
        try {
            val failure = assertNotNull(createTokenRefresh(client).refresh("sent-token").exceptionOrNull())
            assertFalse(failure is RefreshRejectedException)
            assertFalse(failure.message.orEmpty().contains("new-access"))
            assertFalse(failure.message.orEmpty().contains("sent-token"))
        } finally { client.close() }
    }

    @Test fun onlyHttp401RejectsTheSession() = runBlocking {
        for (status in listOf(400, 401, 403, 429, 500, 502, 503)) {
            val client = client { respond("not-json", HttpStatusCode.fromValue(status)) }
            try {
                val failure = assertNotNull(createTokenRefresh(client).refresh("r").exceptionOrNull())
                assertEquals(status == 401, failure is RefreshRejectedException, "HTTP $status")
            } finally { client.close() }
        }
    }

    @Test fun transportFailureIsTemporary() = runBlocking {
        val client = client { throw IllegalStateException("transport unavailable") }
        try {
            val failure = assertNotNull(createTokenRefresh(client).refresh("r").exceptionOrNull())
            assertFalse(failure is RefreshRejectedException)
        } finally { client.close() }
    }

    @Test fun rejectedOrEmptyEnvelopeDoesNotRejectTheSession() = runBlocking {
        val rejected = client { respond("""{"isSuccess":false,"message":"Token revoked","data":null}""", HttpStatusCode.OK, json) }
        val empty = client { respond("""{"isSuccess":true,"data":null}""", HttpStatusCode.OK, json) }
        try {
            val failure = assertNotNull(createTokenRefresh(rejected).refresh("r").exceptionOrNull())
            assertEquals("Token revoked", failure.message)
            assertFalse(failure is RefreshRejectedException)
            assertTrue(createTokenRefresh(empty).refresh("r").isFailure)
        } finally { rejected.close(); empty.close() }
    }

    @Test fun blankTokenFailsWithoutAnyRequest() = runBlocking {
        val client = client { tokens() }
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

    @Test fun credentialDtosDoNotRevealSecretsInToString() {
        assertEquals("NativeRefreshRequest([redacted])", NativeRefreshRequest("secret").toString())
        assertEquals("NativeTokensDto([redacted])", NativeTokensDto("access", "refresh", "a", "r").toString())
        assertEquals("TokensDto([redacted])", TokensDto("access", "refresh").toString())
        assertFalse(LoginRequest("email", "secret").toString().contains("secret"))
    }
}
