package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class RequestObservationTest {
    private class Session(var tokens: SessionTokens?, private val rotate: () -> SessionTokens) : BearerSession {
        override suspend fun load() = tokens?.let { SessionTokens(it.accessToken, it.refreshToken) }
        override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens }
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
            if (this.tokens != expected) return false
            this.tokens = tokens
            return true
        }
        override suspend fun refresh(refreshToken: String): Result<SessionTokens> = runCatching(rotate)
    }

    @Test fun authRefreshAndRetryAreOneLogicalResult() = runBlocking {
        var requests = 0
        var refreshes = 0
        val session = Session(SessionTokens("old", "refresh")) { refreshes++; SessionTokens("new", "refresh") }
        val results = mutableListOf<Pair<Int?, Boolean>>()
        val client = HttpClient(MockEngine { request ->
            requests++
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            else respond("ok", HttpStatusCode.OK)
        }) {
            configureBearerAuthentication("https://api.example.test", session, SessionRefreshCoordinator(session)) {
                it.call.request.attributes.getOrNull(logicalRequestKey)?.authRetried = true
            }
        }
        client.observeRequests("https://api.example.test") {
            RequestObserver { category, method ->
                assertEquals("shops", category); assertEquals("GET", method)
                RequestObservation { status, error, duration, retried ->
                    assertNull(error); assertTrue(duration >= 0); results += status to retried
                }
            }
        }
        try {
            client.get("https://api.example.test/api/shops/private-id?query=secret")
            assertEquals(2, requests)
            assertEquals(1, refreshes)
            assertEquals(listOf<Pair<Int?, Boolean>>(200 to true), results)
        } finally { client.close() }
    }

    @Test fun externalUploadsAndCancelledRequestsDoNotProduceResults() = runBlocking {
        var observations = 0
        val observer = RequestObserver { _, _ -> RequestObservation { _, _, _, _ -> observations++ } }
        val external = HttpClient(MockEngine { respond("ok") })
        external.observeRequests("https://api.example.test") { observer }
        try { external.get("https://storage.example.test/private-file?signature=secret"); assertEquals(0, observations) }
        finally { external.close() }
        val cancelled = HttpClient(MockEngine { throw CancellationException("cancel") })
        cancelled.observeRequests("https://api.example.test") { observer }
        try {
            assertFailsWith<CancellationException> { cancelled.get("https://api.example.test/api/shops") }
            assertEquals(0, observations)
        } finally { cancelled.close() }
    }

    @Test fun observerFailureCannotBreakApplicationRequest() = runBlocking {
        val client = HttpClient(MockEngine { respond("ok") })
        client.observeRequests("https://api.example.test") { RequestObserver { _, _ -> error("analytics failure") } }
        try { assertEquals(HttpStatusCode.OK, client.get("https://api.example.test/api/shops").status) }
        finally { client.close() }
    }

    @Test fun aServerErrorIsReportedWithItsStatusAndTheFailure(): Unit = runBlocking {
        val results = mutableListOf<Pair<Int?, Throwable?>>()
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }) { installServerErrorValidation() }
        client.observeRequests("https://api.example.test") {
            RequestObserver { _, _ -> RequestObservation { status, error, _, _ -> results += status to error } }
        }
        try {
            assertFailsWith<ServiceUnavailableException> { client.get("https://api.example.test/api/shops") }
            assertEquals(503, results.single().first)
            assertIs<ServiceUnavailableException>(results.single().second)
        } finally { client.close() }
    }
}
