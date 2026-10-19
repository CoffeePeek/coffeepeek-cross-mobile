package com.coffeepeek.data.repository

import com.coffeepeek.api.service.AuthService
import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppleLoginPersistenceTest {
    private val accessToken = "header.eyJzdWIiOiJhcHBsZS11c2VyIn0.signature"
    private val successBody =
        """{"isSuccess":true,"data":{"accessToken":"$accessToken","refreshToken":"refresh-token"}}"""

    @Test
    fun successfulAppleSessionIsPersistedWithJwtUserId() = runBlocking {
        val sessions = AppleTestSessionRepository()
        val client = client(successBody)
        try {
            val session = repository(client, sessions).appleLogin("challenge", "native-token").getOrThrow()
            assertEquals(Session(accessToken, "refresh-token", "apple-user"), session)
            assertEquals(listOf<Session?>(session), sessions.savedSessions)
            assertEquals(session, sessions.peekSession())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectedApiResponseNeverPersistsSessionOrReceivesPersistenceMarker() = runBlocking {
        val sessions = AppleTestSessionRepository()
        val client = client("""{"isSuccess":false,"message":"Expired challenge"}""")
        try {
            val error = repository(client, sessions).appleLogin("challenge", "native-token").exceptionOrNull()!!
            assertTrue(sessions.savedSessions.isEmpty())
            assertFalse(error.message.orEmpty().contains("ASI-P"))
        } finally {
            client.close()
        }
    }

    @Test
    fun failedPersistenceReportsItsStageWithoutExposingKeychainOrTokenDetails() = runBlocking {
        val failure = IllegalStateException("Keychain failed for native-token / refresh-token / $accessToken")
        val sessions = AppleTestSessionRepository(saveFailure = failure)
        val client = client(successBody)
        try {
            val error = repository(client, sessions).appleLogin("challenge", "native-token").exceptionOrNull()!!
            assertTrue(error.message.orEmpty().contains("[ASI-P]"))
            assertSame(failure, error.cause)
            for (sensitive in listOf("Keychain", "native-token", "refresh-token", accessToken)) {
                assertFalse(error.message.orEmpty().contains(sensitive))
            }
            assertEquals(1, sessions.savedSessions.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellationDuringPersistenceIsRethrownUnchanged() = runBlocking {
        val cancellation = CancellationException("Login cancelled")
        val sessions = AppleTestSessionRepository(saveFailure = cancellation)
        val client = client(successBody)
        try {
            val thrown = assertFailsWith<CancellationException> {
                repository(client, sessions).appleLogin("challenge", "native-token")
            }
            assertSame(cancellation, thrown)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellationDuringApiExchangeIsRethrownWithoutPersistence() = runBlocking {
        val cancellation = CancellationException("Exchange cancelled")
        val sessions = AppleTestSessionRepository()
        val client = HttpClient(MockEngine { throw cancellation }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val thrown = assertFailsWith<CancellationException> {
                repository(client, sessions).appleLogin("challenge", "native-token")
            }
            // Ktor may copy the exception when recovering coroutine stack traces.
            assertEquals(cancellation.message, thrown.message)
            assertTrue(sessions.savedSessions.isEmpty())
        } finally {
            client.close()
        }
    }

    private fun client(body: String) = HttpClient(MockEngine {
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun repository(client: HttpClient, sessions: SessionRepository) = AuthRepositoryImpl(
        AuthService(client, client),
        sessions,
        UserSessionCleaner(sessions, UnusedAppleTestFavorites, "", ""),
    )
}

private class AppleTestSessionRepository(
    private val saveFailure: Throwable? = null,
) : SessionRepository {
    private val session = MutableStateFlow<Session?>(null)
    val savedSessions = mutableListOf<Session?>()

    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = session != null && session == this.session.value
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) { applySession(session) }
    override suspend fun saveSession(session: Session?) {
        savedSessions += session
        saveFailure?.let { throw it }
        applySession(session)
    }
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = session.value != null
}

private object UnusedAppleTestFavorites : FavoriteRepository {
    override suspend fun getFavoriteIds(): Set<String> = error("Not used by Apple login")
    override suspend fun isFavorite(shopId: String): Boolean = error("Not used by Apple login")
    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("Not used by Apple login")
    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Not used by Apple login")
    override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Not used by Apple login")
    override suspend fun clearAll(): Unit = error("Not used by Apple login")
}
