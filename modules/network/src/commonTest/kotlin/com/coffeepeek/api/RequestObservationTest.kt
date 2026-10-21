package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
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
    @Test fun authRefreshAndRetryAreOneLogicalResult() = runBlocking {
        var tokens = AuthResp("old", "refresh")
        var requests = 0
        var refreshes = 0
        val results = mutableListOf<Pair<Int?, Boolean>>()
        val client = HttpClient(MockEngine { request ->
            requests++
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            else respond("ok", HttpStatusCode.OK)
        }) {
            configureSessionAuthentication("https://api.example.test", { tokens }, { expected, updated ->
                if (tokens != expected) false else { updated?.let { tokens = it }; true }
            }) {
                refreshes++; AuthResp("new", "refresh")
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
}
