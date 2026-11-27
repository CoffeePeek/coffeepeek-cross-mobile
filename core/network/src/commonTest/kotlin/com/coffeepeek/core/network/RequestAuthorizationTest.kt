package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class RequestAuthorizationTest {
    @Test fun permissionLostDuringRefreshCannotSendSecondRequest() = runBlocking {
        var allowed = true
        var requests = 0
        val session = GuardSession(onRefresh = { allowed = false })
        val client = HttpClient(MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }) {
            configureBearerAuthentication("https://api.example.test", session)
        }
        try {
            assertFailsWith<CancellationException> {
                client.get("https://api.example.test/api/v1/attribution/touches") { authorizeWhile { allowed } }
            }
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test fun accountChangedWhileCredentialsLoadStopsBeforeTransport() = runBlocking {
        var allowed = true
        var requests = 0
        val session = GuardSession(onLoad = { allowed = false })
        val client = HttpClient(MockEngine { requests++; respond("ok") }) {
            configureBearerAuthentication("https://api.example.test", session)
        }
        try {
            assertFailsWith<CancellationException> {
                client.get("https://api.example.test/api/v1/attribution/claim") { authorizeWhile { allowed } }
            }
            assertEquals(0, requests)
        } finally { client.close() }
    }
}

private class GuardSession(private val onLoad: () -> Unit = {}, private val onRefresh: () -> Unit = {}) : BearerSession {
    private var tokens: SessionTokens? = SessionTokens("old", "refresh")
    override suspend fun load(): SessionTokens? {
        onLoad()
        return tokens
    }
    override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens }
    override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
        if (this.tokens != expected) return false
        this.tokens = tokens
        return true
    }
    override suspend fun refresh(refreshToken: String): Result<SessionTokens> {
        onRefresh()
        return Result.success(SessionTokens("new", "refresh"))
    }
}
