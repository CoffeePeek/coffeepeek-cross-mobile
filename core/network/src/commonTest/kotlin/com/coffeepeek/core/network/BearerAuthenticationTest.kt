package com.coffeepeek.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BearerAuthenticationTest {
    private class Session(
        var tokens: SessionTokens? = SessionTokens("old", "refresh"),
        val refreshBlock: suspend () -> Result<SessionTokens> = { Result.success(SessionTokens("new", "next")) },
    ) : BearerSession {
        var refreshes = 0
        var saves = 0
        override suspend fun load() = tokens
        override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens; saves++ }
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
            if (this.tokens !== expected) return false
            save(tokens)
            return true
        }
        override suspend fun refresh(refreshToken: String): Result<SessionTokens> {
            assertEquals("refresh", refreshToken)
            refreshes++
            return refreshBlock()
        }
    }

    @Test fun temporaryRefreshFailurePreservesCredentials() = runBlocking {
        val failure = IllegalStateException("offline")
        val session = Session(refreshBlock = { Result.failure(failure) })
        val original = session.tokens
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertSame(original, session.tokens)
            assertEquals(0, session.saves)
        } finally { client.close(); engine.close() }
    }

    @Test fun logoutDuringRefreshCannotRestoreCredentials() = runBlocking {
        lateinit var session: Session
        session = Session(refreshBlock = { session.tokens = null; Result.success(SessionTokens("new", "next")) })
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertEquals(null, session.tokens)
            assertEquals(0, session.saves)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun retriesOnceAndPersistsRefreshedTokens(): Unit = runBlocking {
        val session = Session()
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized)
            else {
                assertEquals("Bearer new", request.headers[HttpHeaders.Authorization])
                respond("ok")
            }
        }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isSuccess)
            assertEquals(2, requests)
            assertEquals(1, session.refreshes)
            assertEquals("new", session.tokens?.accessToken)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun repeatedUnauthorizedDoesNotLoop(): Unit = runBlocking {
        val session = Session()
        var requests = 0
        val engine = MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertEquals(2, requests)
            assertEquals(1, session.refreshes)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun failedRefreshClearsSessionWithoutRetry(): Unit = runBlocking {
        val session = Session(refreshBlock = { Result.failure(RefreshRejectedException()) })
        var requests = 0
        val engine = MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertEquals(null, session.tokens)
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun refreshCancellationPreservesSession(): Unit = runBlocking {
        val cancellation = CancellationException("cancelled refresh")
        val session = Session(refreshBlock = { Result.failure(cancellation) })
        val original = session.tokens
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertFailsWith<CancellationException> { requestResult { client.get("/items") } }
            assertSame(original, session.tokens)
            assertEquals(0, session.saves)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun foreignOriginNeverReceivesCredentialsOrRefresh(): Unit = runBlocking {
        val session = Session()
        val engine = MockEngine { request ->
            assertEquals(null, request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.Unauthorized)
        }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            for (url in listOf("https://storage.example.com/file", "https://api.example.com:8443/file")) {
                assertTrue(requestResult { client.get(url) }.isFailure)
            }
            assertEquals(0, session.refreshes)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun parallelUnauthorizedSharesOneRefresh(): Unit = runBlocking {
        withTimeout(5_000) {
            val firstOldRequest = CompletableDeferred<Unit>()
            val secondOldRequest = CompletableDeferred<Unit>()
            val session = Session(refreshBlock = {
                firstOldRequest.await()
                secondOldRequest.await()
                Result.success(SessionTokens("new", "next"))
            })
            val engine = MockEngine { request ->
                if (request.headers[HttpHeaders.Authorization] == "Bearer old") {
                    if (request.url.encodedPath == "/one") firstOldRequest.complete(Unit)
                    else secondOldRequest.complete(Unit)
                    respond("", HttpStatusCode.Unauthorized)
                } else respond("ok")
            }
            val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
            try {
                listOf(async { client.get("/one") }, async { client.get("/two") }).awaitAll()
                assertEquals(1, session.refreshes)
                assertEquals(1, session.saves)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun externalSessionChangeCanInvalidateCachedTokens(): Unit = runBlocking {
        val session = Session()
        val headers = mutableListOf<String?>()
        val engine = MockEngine { request -> headers += request.headers[HttpHeaders.Authorization]; respond("ok") }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            client.get("/items")
            session.tokens = null
            client.clearBearerTokenCache()
            client.get("/items")
            assertEquals(listOf("Bearer old", null), headers)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun missingRefreshClearsTokensAndGuestDoesNotRefresh(): Unit = runBlocking {
        for (tokens in listOf(null, SessionTokens("old", ""))) {
            val session = Session(tokens)
            var requests = 0
            val engine = MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }
            val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
            try {
                assertTrue(requestResult { client.get("/items") }.isFailure)
                assertEquals(0, session.refreshes)
                assertEquals(null, session.tokens)
                assertEquals(1, requests)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun redirectToForeignHostDoesNotForwardCredentials(): Unit = runBlocking {
        val session = Session()
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            if (request.url.host == "api.example.com") {
                assertEquals("Bearer old", request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://storage.example.com/file"))
            } else {
                assertEquals(null, request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.Unauthorized)
            }
        }
        val client = HttpClientFactory(engine).authenticatedApi("https://api.example.com", session)
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertEquals(2, requests)
            assertEquals(0, session.refreshes)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun insecureAuthenticationIsRejectedAndTokensAreRedacted() {
        val engine = MockEngine { respond("ok") }
        try {
            assertFailsWith<IllegalArgumentException> {
                HttpClientFactory(engine).authenticatedApi("http://api.example.com", Session())
            }
            assertEquals("SessionTokens(redacted)", SessionTokens("secret", "other-secret").toString())
        } finally { engine.close() }
    }
}
