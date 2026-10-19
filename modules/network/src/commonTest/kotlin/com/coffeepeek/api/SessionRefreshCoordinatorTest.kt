package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

class SessionRefreshCoordinatorTest {
    @Test fun apiAndImageConcurrent401RotateRefreshTokenOnce() = runBlocking {
        var tokens: AuthResp? = AuthResp("old-access", "old-refresh")
        var rotations = 0
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val save: suspend (AuthResp, AuthResp?) -> Boolean = { expected, updated ->
            if (tokens != expected) false else { tokens = updated; true }
        }
        val coordinator = SessionRefreshCoordinator({ tokens }, save) {
            rotations++; started.complete(Unit); release.await(); AuthResp("new-access", "new-refresh")
        }
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") respond("ok")
            else respond("denied", HttpStatusCode.Unauthorized)
        }
        fun client() = HttpClient(engine) {
            configureSessionAuthentication("https://api.example", { tokens }, save, coordinator)
        }.also { it.readCurrentSessionForRequests() }
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
            assertEquals("new-refresh", tokens?.refreshToken)
        } finally { api.close(); images.close() }
    }

    @Test fun accountSwitchDuringRotationNeverPersistsOrRetriesOtherCredentials() = runBlocking {
        val initial = AuthResp("a-access", "a-refresh")
        val replacement = AuthResp("b-access", "b-refresh")
        var tokens: AuthResp? = initial
        val finish = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val coordinator = SessionRefreshCoordinator({ tokens }, { expected, updated ->
            if (tokens != expected) false else { tokens = updated; true }
        }) {
            started.complete(Unit); finish.await(); AuthResp("rotated-a", "rotated-refresh")
        }
        val inFlight = async { coordinator.refresh(initial.refreshToken, initial.accessToken) }
        started.await(); tokens = replacement; finish.complete(Unit)
        assertFailsWith<CancellationException> { inFlight.await() }
        assertEquals(replacement, tokens)
        assertFailsWith<CancellationException> { coordinator.refresh(initial.refreshToken, initial.accessToken) }
        Unit
    }

    @Test fun delayedOld401UsesAlreadySavedRotationAndNeverRefreshesNewToken() = runBlocking {
        var tokens: AuthResp? = AuthResp("old-access", "old-refresh")
        var rotations = 0
        val firstSaved = CompletableDeferred<Unit>()
        val delayedStarted = CompletableDeferred<Unit>()
        val save: suspend (AuthResp, AuthResp?) -> Boolean = { expected, updated ->
            if (tokens != expected) false else {
                tokens = updated
                firstSaved.complete(Unit)
                true
            }
        }
        val coordinator = SessionRefreshCoordinator({ tokens }, save) {
            rotations++; AuthResp("new-access", "new-refresh")
        }
        val engine = MockEngine { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") respond("ok")
            else {
                if (request.url.encodedPath.endsWith("delayed")) { delayedStarted.complete(Unit); firstSaved.await() }
                respond("denied", HttpStatusCode.Unauthorized)
            }
        }
        fun client() = HttpClient(engine) {
            configureSessionAuthentication("https://api.example", { tokens }, save, coordinator)
        }.also { it.readCurrentSessionForRequests() }
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
        val initial = AuthResp("a-access", "a-refresh")
        val replacement = AuthResp("b-access", "b-refresh")
        var tokens: AuthResp? = initial
        val coordinator = SessionRefreshCoordinator({ tokens }, { expected, _ ->
            assertEquals(initial, expected)
            tokens = replacement
            false
        }) { AuthResp("rotated-a", "rotated-refresh") }

        assertFailsWith<CancellationException> { coordinator.refresh(initial.refreshToken, initial.accessToken) }
        assertEquals(replacement, tokens)
        assertFailsWith<CancellationException> { coordinator.refresh(initial.refreshToken, initial.accessToken) }
        Unit
    }
}
