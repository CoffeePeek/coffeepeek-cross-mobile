package com.coffeepeek.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs

class NetworkDiagnosticsTest {
    @Test
    fun httpFailureReportsNumericStatusWithoutBody(): Unit = runBlocking {
        val events = mutableListOf<NetworkDiagnostic>()
        val engine = MockEngine { respond("error-secret", HttpStatusCode.ServiceUnavailable) }
        val client = HttpClientFactory(engine).api("https://example.com") { configureNetworkDiagnostics(events::add) }
        try {
            assertTrue(requestResult { client.get("/items") }.isFailure)
            assertEquals(503, events.last().statusCode)
            assertFalse(events.toString().contains("error-secret"))
        } finally { client.close(); engine.close() }
    }

    @Test
    fun secretsAreNotPresentInDiagnosticEvents(): Unit = runBlocking {
        val events = mutableListOf<NetworkDiagnostic>()
        val engine = MockEngine { respond("response-secret") }
        val client = HttpClientFactory(engine).api("https://example.com") { configureNetworkDiagnostics(events::add) }
        try {
            client.post("/path-secret?signature=query-secret") {
                header(HttpHeaders.Authorization, "Bearer header-secret")
                header(HttpHeaders.Cookie, "cookie-secret")
                setBody("password=body-secret")
            }
            assertEquals(listOf(NetworkDiagnostic.Outcome.STARTED, NetworkDiagnostic.Outcome.RESPONSE), events.map { it.outcome })
            assertEquals(200, events.last().statusCode)
            assertEquals(NetworkDiagnostic.Method.POST, events.first().method)
            assertFalse(events.toString().contains("secret"))
        } finally { client.close(); engine.close() }
    }

    @Test
    fun transportErrorsArePreservedWithoutTheirMessages(): Unit = runBlocking {
        val events = mutableListOf<NetworkDiagnostic>()
        val failure = IllegalStateException("https://example.com/?token=exception-secret")
        val engine = MockEngine { throw failure }
        val client = HttpClientFactory(engine).api("https://example.com") { configureNetworkDiagnostics(events::add) }
        try {
            val resultFailure = assertIs<IllegalStateException>(requestResult { client.get("/items") }.exceptionOrNull())
            assertEquals(failure.message, resultFailure.message)
            assertEquals(NetworkDiagnostic.Outcome.FAILED, events.last().outcome)
            assertFalse(events.toString().contains("exception-secret"))
        } finally { client.close(); engine.close() }
    }

    @Test
    fun cancellationIsReportedAndRethrown(): Unit = runBlocking {
        val events = mutableListOf<NetworkDiagnostic>()
        val engine = MockEngine { throw CancellationException("cancel-secret") }
        val client = HttpClientFactory(engine).api("https://example.com") { configureNetworkDiagnostics(events::add) }
        try {
            assertFailsWith<CancellationException> { requestResult { client.get("/items") } }
            assertEquals(NetworkDiagnostic.Outcome.CANCELLED, events.last().outcome)
            assertFalse(events.toString().contains("cancel-secret"))
        } finally { client.close(); engine.close() }
    }

    @Test
    fun brokenObserverDoesNotChangeRequestResult(): Unit = runBlocking {
        val engine = MockEngine { respond("ok") }
        val client = HttpClientFactory(engine).api("https://example.com") {
            configureNetworkDiagnostics { throw IllegalStateException("logger failure") }
        }
        try { assertTrue(requestResult { client.get("/items") }.isSuccess) }
        finally { client.close(); engine.close() }
    }
}
