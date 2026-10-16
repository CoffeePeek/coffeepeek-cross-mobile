package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.core.network.RefreshRejectedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SessionAuthenticationFailureTest {
    @Test fun temporaryRefreshFailurePreservesTheProductionLegacySession(): Unit = runBlocking {
        val original = AuthResp("access", "refresh")
        var session: AuthResp? = original
        var saves = 0
        val client = HttpClient(MockEngine {
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        }) {
            configureSessionAuthentication("https://example.com", { session }, { expected, updated ->
                if (session != expected) false else { session = updated; saves++; true }
            }, { throw IllegalStateException("Temporary refresh outage") })
        }
        try {
            assertFailsWith<IllegalStateException> { client.get("https://example.com/private") }
            assertEquals(original, session)
            assertEquals(0, saves)
        } finally { client.close() }
    }

    @Test fun definitiveRefreshRejectionClearsOnlyTheMatchingSession(): Unit = runBlocking {
        var session: AuthResp? = AuthResp("access", "refresh")
        val client = HttpClient(MockEngine {
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        }) {
            configureSessionAuthentication("https://example.com", { session }, { expected, updated ->
                if (session != expected) false else { session = updated; true }
            }, { throw RefreshRejectedException() })
        }
        try {
            assertEquals(HttpStatusCode.Unauthorized, client.get("https://example.com/private").status)
            assertNull(session)
        } finally { client.close() }
    }
}
