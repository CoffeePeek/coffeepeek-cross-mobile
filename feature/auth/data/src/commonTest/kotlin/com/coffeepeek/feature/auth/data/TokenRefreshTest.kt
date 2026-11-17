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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenRefreshTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClientFactory(MockEngine { request -> requests += request; handler(request) }).api("https://api.example")

    private fun MockRequestHandleScope.tokens(refresh: String) = respond(
        """{"isSuccess":true,"data":{"accessToken":"new-access","refreshToken":"$refresh","refreshTokenExpiresAt":"2026-12-31T00:00:00Z"}}""",
        HttpStatusCode.OK,
        json,
    )

    @Test fun sendsRefreshTokensAsJsonAndUsesEachRotatedTokenForTheNextExchange() = runBlocking {
        val client = client { tokens("refresh-${requests.size}") }
        try {
            val refresh = createTokenRefresh(client)
            val first = refresh.refresh("old-refresh").getOrThrow()
            val second = refresh.refresh(assertNotNull(first.refreshToken)).getOrThrow()
            assertEquals(SessionTokens("new-access", "refresh-1"), first)
            assertEquals(SessionTokens("new-access", "refresh-2"), second)
            assertEquals(
                listOf("""{"refreshToken":"old-refresh"}""", """{"refreshToken":"refresh-1"}"""),
                requests.map { it.body.toByteArray().decodeToString() },
            )
            for (request in requests) {
                assertEquals(HttpMethod.Put, request.method)
                assertEquals("/api/Tokens/native", request.url.encodedPath)
                assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
                assertNull(request.headers[HttpHeaders.Cookie])
                assertNull(request.headers[HttpHeaders.Authorization])
            }
        } finally {
            client.close()
        }
    }

    @Test fun aSuccessfulResponseWithoutANewRefreshTokenIsAnInvalidResponse() = runBlocking {
        val jsonWithCookie = headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.SetCookie to listOf("refreshToken=cookie-token; Path=/; HttpOnly"),
        )
        val missing = """{"accessToken":"new-access"}"""
        val cases = listOf(
            missing to json,
            missing to jsonWithCookie,
            """{"accessToken":"new-access","refreshToken":" "}""" to json,
            """{"accessToken":"new-access","refreshToken":"old-refresh"}""" to json,
        )
        for ((data, headers) in cases) {
            val client = client { respond("""{"isSuccess":true,"data":$data}""", HttpStatusCode.OK, headers) }
            try {
                val result = createTokenRefresh(client).refresh("old-refresh")
                assertIs<AuthApiException>(result.exceptionOrNull())
            } finally {
                client.close()
            }
        }
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
