package com.coffeepeek.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The shared rotation that API and image clients use; the tokens are compared by value, as the application's store loads fresh copies. */
class SessionRefreshCoordinatorTest {
    private class FakeSession(
        var tokens: SessionTokens?,
        private val rotate: suspend (String) -> SessionTokens,
        private val accept: (SessionTokens, SessionTokens?) -> Boolean = { _, _ -> true },
    ) : BearerSession {
        override suspend fun load() = tokens?.let { SessionTokens(it.accessToken, it.refreshToken) }
        override suspend fun save(tokens: SessionTokens?) { this.tokens = tokens }
        override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean {
            if (this.tokens != expected || !accept(expected, tokens)) return false
            this.tokens = tokens
            return true
        }
        override suspend fun refresh(refreshToken: String): Result<SessionTokens> = runCatching { rotate(refreshToken) }
    }

    @Test fun apiAndImageConcurrent401RotateRefreshTokenOnce() = runBlocking {
        var rotations = 0
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val session = FakeSession(SessionTokens("old-access", "old-refresh"), {
            rotations++; started.complete(Unit); release.await(); SessionTokens("new-access", "new-refresh")
        })
        val coordinator = SessionRefreshCoordinator(session)
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") respond("ok")
            else respond("denied", HttpStatusCode.Unauthorized)
        }
        fun client() = HttpClientFactory(engine).authenticatedApi("https://api.example", session, coordinator = coordinator)
        val api = client(); val images = client()
        try {
            val first = async { api.get("https://api.example/api/v1/feed").status }
            val second = async { images.get("https://api.example/api/v1/check-ins/visit/photos/photo").status }
            started.await()
            assertEquals(1, rotations)
            release.complete(Unit)
            assertEquals(HttpStatusCode.OK, first.await())
            assertEquals(HttpStatusCode.OK, second.await())
            assertEquals(1, rotations)
            assertEquals("new-refresh", session.tokens?.refreshToken)
        } finally { api.close(); images.close() }
    }

    @Test fun accountSwitchDuringRotationNeverPersistsOrRetriesOtherCredentials() = runBlocking {
        val initial = SessionTokens("a-access", "a-refresh")
        val replacement = SessionTokens("b-access", "b-refresh")
        val finish = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val session = FakeSession(initial, { started.complete(Unit); finish.await(); SessionTokens("rotated-a", "rotated-refresh") })
        val coordinator = SessionRefreshCoordinator(session)
        val inFlight = async { coordinator.refresh("a-refresh", "a-access") }
        started.await(); session.tokens = replacement; finish.complete(Unit)
        assertFailsWith<CancellationException> { inFlight.await() }
        assertEquals(replacement, session.tokens)
        assertFailsWith<CancellationException> { coordinator.refresh("a-refresh", "a-access") }
        Unit
    }

    @Test fun delayedOld401UsesAlreadySavedRotationAndNeverRefreshesNewToken() = runBlocking {
        var rotations = 0
        val firstSaved = CompletableDeferred<Unit>()
        val delayedStarted = CompletableDeferred<Unit>()
        val session = FakeSession(SessionTokens("old-access", "old-refresh"), {
            rotations++; SessionTokens("new-access", "new-refresh")
        }, accept = { _, _ -> firstSaved.complete(Unit); true })
        val coordinator = SessionRefreshCoordinator(session)
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") respond("ok")
            else {
                if (request.url.encodedPath.endsWith("delayed")) { delayedStarted.complete(Unit); firstSaved.await() }
                respond("denied", HttpStatusCode.Unauthorized)
            }
        }
        fun client() = HttpClientFactory(engine).authenticatedApi("https://api.example", session, coordinator = coordinator)
        val api = client(); val images = client()
        try {
            val delayed = async { images.get("https://api.example/delayed").status }
            delayedStarted.await()
            assertEquals(HttpStatusCode.OK, api.get("https://api.example/first").status)
            assertEquals(HttpStatusCode.OK, delayed.await())
            assertEquals(1, rotations)
        } finally { api.close(); images.close() }
    }

    @Test fun rejectedAtomicPersistenceNeverReturnsRotatedCredentials() = runBlocking {
        val initial = SessionTokens("a-access", "a-refresh")
        val replacement = SessionTokens("b-access", "b-refresh")
        lateinit var session: FakeSession
        session = FakeSession(initial, { SessionTokens("rotated-a", "rotated-refresh") }, accept = { expected, _ ->
            assertEquals(initial, expected)
            session.tokens = replacement
            false
        })
        val coordinator = SessionRefreshCoordinator(session)

        assertFailsWith<CancellationException> { coordinator.refresh("a-refresh", "a-access") }
        assertEquals(replacement, session.tokens)
        assertFailsWith<CancellationException> { coordinator.refresh("a-refresh", "a-access") }
        Unit
    }
}
