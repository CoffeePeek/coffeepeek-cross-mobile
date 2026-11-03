package com.coffeepeek.admin.di.collections

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CollectionsHttpClientTest {
    @Test fun accountReplacementBeforeSendNeverReachesTransport() = runTest {
        val sessions = Sessions()
        var sends = 0
        val base = HttpClient(MockEngine { sends++; respond("", HttpStatusCode.NoContent) }) {
            defaultRequest { headers[HttpHeaders.Authorization] = "Bearer first-token" }
        }
        val bound = CollectionsHttpClient(base, sessions)
        bound.client.requestPipeline.intercept(HttpRequestPipeline.State) { sessions.current.value = Session("second-token", userId = "second") }
        try {
            assertFailsWith<CancellationException> { bound.client.post("https://fixture.invalid/api/v1/favorite-collections") { bound.pin(this, "first") } }
            assertEquals(0, sends)
        } finally { bound.client.close(); base.close() }
    }

    @Test fun mismatchedBearerCannotWriteEvenWhenTheExpectedAccountIsCurrentAgain() = runTest {
        val sessions = Sessions()
        var sends = 0
        val base = HttpClient(MockEngine { sends++; respond("", HttpStatusCode.NoContent) }) {
            defaultRequest { headers[HttpHeaders.Authorization] = "Bearer second-token" }
        }
        val bound = CollectionsHttpClient(base, sessions)
        try {
            assertFailsWith<CancellationException> { bound.client.delete("https://fixture.invalid/api/v1/favorite-collections/id") { bound.pin(this, "first") } }
            assertEquals(0, sends)
        } finally { bound.client.close(); base.close() }
    }

    @Test fun rotatingCredentialsForTheSameAccountKeepsRequestsWorking() = runTest {
        val sessions = Sessions()
        sessions.current.value = Session("rotated-token", userId = "first")
        var sends = 0
        val base = HttpClient(MockEngine { sends++; respond("", HttpStatusCode.NoContent) }) {
            defaultRequest { headers[HttpHeaders.Authorization] = "Bearer rotated-token" }
        }
        val bound = CollectionsHttpClient(base, sessions)
        try {
            assertEquals(HttpStatusCode.NoContent, bound.client.post("https://fixture.invalid/api/v1/favorite-collections") { bound.pin(this, "first") }.status)
            assertEquals(1, sends)
        } finally { bound.client.close(); base.close() }
    }

    private class Sessions : SessionReader {
        val current = MutableStateFlow<Session?>(Session("first-token", userId = "first"))
        override fun peekSession() = current.value
        override fun isActiveSession(session: Session?) = session != null && session == current.value
        override fun observeSession() = current
        override suspend fun getSession() = current.value
    }
}
