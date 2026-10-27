package com.coffeepeek.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The rules of a session shared by several clients; the transport is a mock, no credentials are real. */
class SharedSessionAuthenticationTest {
    private class FakeSession(
        var tokens: SessionTokens?,
        private val rotate: suspend (String) -> SessionTokens,
    ) : BearerSession {
        var saves = 0
        override suspend fun load() = tokens?.let { SessionTokens(it.accessToken, it.refreshToken) }
        override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens }
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
            if (this.tokens != expected) return false
            this.tokens = tokens; saves++
            return true
        }
        override suspend fun refresh(refreshToken: String): Result<SessionTokens> = runCatching { rotate(refreshToken) }
    }

    private fun MockRequestHandleScope.unauthorized() = respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))

    @Test fun temporaryRefreshFailurePreservesTheSession(): Unit = runBlocking {
        val original = SessionTokens("access", "refresh")
        val session = FakeSession(original) { throw IllegalStateException("Temporary refresh outage") }
        val engine = MockEngine { unauthorized() }
        val client = HttpClientFactory(engine).authenticatedApi("https://example.com", session, coordinator = SessionRefreshCoordinator(session))
        try {
            assertFailsWith<IllegalStateException> { client.get("https://example.com/private") }
            assertEquals(original, session.tokens)
            assertEquals(0, session.saves)
        } finally { client.close() }
    }

    @Test fun definitiveRefreshRejectionClearsOnlyTheMatchingSession(): Unit = runBlocking {
        val session = FakeSession(SessionTokens("access", "refresh")) { throw RefreshRejectedException() }
        val engine = MockEngine { unauthorized() }
        val client = HttpClientFactory(engine).authenticatedApi("https://example.com", session, coordinator = SessionRefreshCoordinator(session))
        try {
            // The final 401 reaches the caller as a failure (the core transport expects success); the session is cleared.
            assertEquals(HttpStatusCode.Unauthorized, assertFailsWith<ClientRequestException> { client.get("https://example.com/private") }.response.status)
            assertNull(session.tokens)
        } finally { client.close() }
    }

    @Test fun aRejectionWithoutARefreshTokenClearsTheRejectedSessionOnly(): Unit = runBlocking {
        val session = FakeSession(SessionTokens("access", "")) { error("No exchange without a refresh token") }
        val engine = MockEngine { unauthorized() }
        val client = HttpClientFactory(engine).authenticatedApi("https://example.com", session, coordinator = SessionRefreshCoordinator(session))
        try {
            // The final 401 reaches the caller as a failure (the core transport expects success); the session is cleared.
            assertEquals(HttpStatusCode.Unauthorized, assertFailsWith<ClientRequestException> { client.get("https://example.com/private") }.response.status)
            assertNull(session.tokens)
        } finally { client.close() }
    }

    @Test fun everyRequestReadsTheCurrentSessionAfterALoginOrLogout(): Unit = runBlocking {
        val session = FakeSession(SessionTokens("first", "refresh")) { error("No refresh expected") }
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request -> seen += request.headers[HttpHeaders.Authorization]; respond("ok") }
        val client = HttpClientFactory(engine).authenticatedApi("https://example.com", session, coordinator = SessionRefreshCoordinator(session))
        try {
            client.get("https://example.com/a")
            session.tokens = SessionTokens("second", "refresh")
            client.get("https://example.com/b")
            session.tokens = null
            client.get("https://example.com/c")
            assertEquals(listOf("Bearer first", "Bearer second", null), seen)
        } finally { client.close() }
    }

    @Test fun theRetryObserverIsToldOfEveryRejectionThatStartsARefresh(): Unit = runBlocking {
        val session = FakeSession(SessionTokens("old", "refresh")) { SessionTokens("new", "next") }
        val retries = mutableListOf<HttpResponse>()
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer new") respond("ok") else unauthorized()
        }
        val client = HttpClientFactory(engine).authenticatedApi(
            "https://example.com", session, coordinator = SessionRefreshCoordinator(session), onAuthRetry = { retries += it },
        )
        try {
            assertEquals(HttpStatusCode.OK, client.get("https://example.com/private").status)
            assertTrue(retries.isNotEmpty())
            assertTrue(retries.all { it.status == HttpStatusCode.Unauthorized })
        } finally { client.close() }
    }

    @Test fun aPlainHttpBaseIsAcceptedOnlyWhenTheCallerAsksForIt(): Unit = runBlocking {
        val session = FakeSession(SessionTokens("access", "refresh")) { error("unused") }
        assertFailsWith<IllegalArgumentException> { HttpClientFactory(MockEngine { respond("ok") }).authenticatedApi("http://example.com", session) }
        val client = HttpClientFactory(MockEngine { respond("ok") }).authenticatedApi("http://example.com", session, requireHttps = false)
        client.close()
    }
}
