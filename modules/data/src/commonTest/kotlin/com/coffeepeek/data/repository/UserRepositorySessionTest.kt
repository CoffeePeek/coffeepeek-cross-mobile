package com.coffeepeek.data.repository

import com.coffeepeek.api.service.PhotoApiService
import com.coffeepeek.api.service.UserApiService
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRepositorySessionTest {
    @Test
    fun cachedProfileIsRejectedSynchronouslyWhenLoginLifetimeChanges() = runBlocking {
        val fixture = QueuedProfileFixture()
        try {
            fixture.assertTokenRefreshKeepsCache()
            fixture.assertLogoutRejectsCacheBeforeCollectorRuns()
            fixture.assertSameOwnerReloginFetchesAgain()
            fixture.assertAccountSwitchFetchesAgain()
        } finally { fixture.close() }
    }

    @Test
    fun lateProfileCannotRestoreLoggedOutUserOrOverwriteNextAccount() = runBlocking {
        for (nextUserId in listOf(null, "new-user", "old-user")) {
            val oldSession = Session("old-token", userId = "old-user")
            val sessions = TestSessionRepository(oldSession)
            val started = CompletableDeferred<Unit>()
            val complete = CompletableDeferred<Unit>()
            var requestCount = 0
            val client = HttpClient(MockEngine {
                val request = ++requestCount
                if (request == 2) {
                    started.complete(Unit)
                    complete.await()
                }
                val name = if (request == 3) "New user" else "Old user"
                respond("""{"isSuccess":true,"message":"OK","data":{"userName":"$name","email":"user@example.com"}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }) {
                install(ContentNegotiation) { json() }
            }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val repository = UserRepositoryImpl(UserApiService(client), PhotoRepositoryImpl(PhotoApiService(client, client)), sessions, scope)
                assertEquals("Old user", repository.refreshProfile().getOrThrow().userName)
                val pending = async { repository.refreshProfile() }
                started.await()
                if (nextUserId == "old-user") {
                    sessions.saveSession(oldSession.copy(accessToken = "refreshed-token"))
                } else {
                    sessions.saveSession(null)
                    assertNull(repository.observeProfile().value)
                }
                if (nextUserId == "new-user") {
                    sessions.saveSession(Session("new-token", userId = "new-user"))
                    assertEquals("New user", repository.getMe().getOrThrow().userName)
                }
                complete.complete(Unit)
                val result = pending.await()
                if (nextUserId == "old-user") {
                    assertEquals("Old user", result.getOrThrow().userName)
                } else {
                    assertTrue(result.isFailure)
                }
                if (nextUserId == "new-user") assertEquals("New user", repository.observeProfile().value?.userName)
                else if (nextUserId == null) {
                    assertNull(repository.observeProfile().value)
                    assertTrue(repository.getMe().isFailure)
                    assertEquals(2, requestCount)
                }
            } finally {
                scope.cancel()
                client.close()
            }
        }
    }
    @Test
    fun lateProfileWritesNeverTouchTheCachedProfileOfAnotherAccount() = runBlocking {
        val sessions = TestSessionRepository(Session("a-token", userId = "a"))
        var signedIn = "A"
        val gate = CompletableDeferred<Unit>()
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val client = HttpClient(MockEngine { request ->
            when {
                request.method.value == "GET" ->
                    respond("""{"isSuccess":true,"message":"OK","data":{"userName":"$signedIn","email":"x@example.com"}}""", headers = json)
                request.url.encodedPath.endsWith("/username") -> {
                    gate.await()
                    respond("""{"isSuccess":true,"message":"OK","data":{"username":"Renamed"}}""", headers = json)
                }
                else -> {
                    gate.await()
                    respond("""{"isSuccess":true,"message":"OK"}""", headers = json)
                }
            }
        }) { install(ContentNegotiation) { json() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = UserRepositoryImpl(UserApiService(client), PhotoRepositoryImpl(PhotoApiService(client, client)), sessions, scope)
            assertEquals("A", repository.refreshProfile().getOrThrow().userName)
            val rename = async(start = CoroutineStart.UNDISPATCHED) { repository.updateUsername("Renamed") }
            val about = async(start = CoroutineStart.UNDISPATCHED) { repository.updateAbout("About of A") }
            // Another account signs in and loads its own profile while the writes of the first one are still pending.
            signedIn = "B"
            sessions.saveSession(Session("b-token", userId = "b"))
            assertEquals("B", repository.getMe().getOrThrow().userName)
            gate.complete(Unit)
            assertTrue(rename.await().isFailure)
            assertTrue(about.await().isFailure)
            assertEquals("B", repository.observeProfile().value?.userName)
            assertNull(repository.observeProfile().value?.about)
        } finally {
            scope.cancel()
            client.close()
        }
    }

    @Test
    fun profileWritesStillApplyAfterATokenRefreshOfTheSameAccount() = runBlocking {
        val old = Session("old-token", userId = "a")
        val sessions = TestSessionRepository(old)
        val gate = CompletableDeferred<Unit>()
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val client = HttpClient(MockEngine { request ->
            if (request.method.value == "GET") respond("""{"isSuccess":true,"message":"OK","data":{"userName":"A","email":"x@example.com"}}""", headers = json)
            else { gate.await(); respond("""{"isSuccess":true,"message":"OK"}""", headers = json) }
        }) { install(ContentNegotiation) { json() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = UserRepositoryImpl(UserApiService(client), PhotoRepositoryImpl(PhotoApiService(client, client)), sessions, scope)
            repository.refreshProfile().getOrThrow()
            val about = async(start = CoroutineStart.UNDISPATCHED) { repository.updateAbout("Fresh about") }
            sessions.saveSession(old.copy(accessToken = "refreshed-token"))
            gate.complete(Unit)
            assertTrue(about.await().isSuccess)
            assertEquals("Fresh about", repository.observeProfile().value?.about)
        } finally {
            scope.cancel()
            client.close()
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private class QueuedProfileFixture {
    val sessions = TestSessionRepository(Session("first-token", userId = "a"))
    var requests = 0
    val client = HttpClient(MockEngine {
        val name = when (++requests) { 1 -> "First login"; 2 -> "Second login"; else -> "Other account" }
        respond("""{"isSuccess":true,"message":"OK","data":{"userName":"$name","email":"x@example.com"}}""",
            headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json() } }
    // The session collector deliberately stays queued throughout the synchronous cache checks.
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher())
    val repository = UserRepositoryImpl(UserApiService(client), PhotoRepositoryImpl(PhotoApiService(client, client)), sessions, scope)

    suspend fun assertTokenRefreshKeepsCache() {
        assertEquals("First login", repository.refreshProfile().getOrThrow().userName)
        val firstGeneration = sessions.accountGeneration()
        sessions.saveSession(Session("refreshed-token", userId = "a"))
        assertEquals(firstGeneration, sessions.accountGeneration())
        assertEquals("First login", repository.cachedProfile()?.userName)
        assertEquals("First login", repository.getMe().getOrThrow().userName)
        assertEquals(1, requests)
    }

    suspend fun assertLogoutRejectsCacheBeforeCollectorRuns() {
        sessions.saveSession(null)
        assertNull(repository.cachedProfile())
        assertEquals("First login", repository.observeProfile().value?.userName)
        assertTrue(repository.getMe().isFailure)
        assertEquals(1, requests)
    }

    suspend fun assertSameOwnerReloginFetchesAgain() {
        sessions.saveSession(Session("second-login-token", userId = "a"))
        assertNull(repository.cachedProfile())
        assertEquals("Second login", repository.getMe().getOrThrow().userName)
        assertEquals(2, requests)
    }

    suspend fun assertAccountSwitchFetchesAgain() {
        sessions.saveSession(Session("other-token", userId = "b"))
        assertNull(repository.cachedProfile())
        assertEquals("Other account", repository.getMe().getOrThrow().userName)
        assertEquals(3, requests)
    }

    fun close() { scope.cancel(); client.close() }
}

private class TestSessionRepository(initial: Session?) : SessionRepository {
    private val session = MutableStateFlow(initial)
    private var generation = if (initial == null) 0L else 1L
    override fun accountGeneration() = generation
    override fun peekSession() = session.value
    override fun applySession(session: Session?) {
        val current = this.session.value
        val sameAccount = current?.userId != null && current.userId == session?.userId
        if (!sameAccount) generation++
        this.session.value = session
    }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) { applySession(session) }
    override suspend fun saveSession(session: Session?) { applySession(session) }
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = session.value != null
}
