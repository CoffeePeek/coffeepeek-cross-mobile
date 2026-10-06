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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRepositorySessionTest {
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
}

private class TestSessionRepository(initial: Session?) : SessionRepository {
    private val session = MutableStateFlow(initial)
    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) { applySession(session) }
    override suspend fun saveSession(session: Session?) { applySession(session) }
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = session.value != null
}
