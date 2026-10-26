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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Password and Google sign-in persistence, account switching and sign-out, as the legacy repository does them today. */
class AuthRepositoryImplTest {
    // JWT payloads {"sub":"user-a"} and {"sub":"user-b"}
    private val tokenA = "header.eyJzdWIiOiJ1c2VyLWEifQ.signature"
    private val tokenB = "header.eyJzdWIiOiJ1c2VyLWIifQ.signature"
    private fun success(token: String) = """{"isSuccess":true,"data":{"accessToken":"$token","refreshToken":"refresh"}}"""

    private class Events {
        val log = mutableListOf<String>()
    }

    private inner class Fixture(
        previous: Session?,
        favoritesFailure: Throwable? = null,
        private val respond: (HttpMethod, String) -> Pair<HttpStatusCode, String>,
    ) {
        val events = Events()
        val sessions = Sessions(previous, events)
        val favorites = Favorites(events, favoritesFailure)
        var refreshCookie: String? = null
        val client = HttpClient(MockEngine { request ->
            events.log += "remote:${request.method.value} ${request.url.encodedPath}"
            refreshCookie = request.headers[HttpHeaders.Cookie]
            val (status, body) = respond(request.method, request.url.encodedPath)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val repository = AuthRepositoryImpl(
            AuthService(client, client), sessions,
            UserSessionCleaner(sessions, favorites, "", "", clearCaches = { events.log += "caches" }),
        )
    }

    @Test fun loginSavesTheSessionWithTheUserIdOfTheTokenAndKeepsDataOfTheSameUser() = runBlocking {
        val fixture = Fixture(Session("old", "r", "user-a")) { _, _ -> HttpStatusCode.OK to success(tokenA) }
        val session = fixture.repository.login("a@example.com", "secret1").getOrThrow()
        assertEquals(Session(tokenA, "refresh", "user-a"), session)
        assertEquals(listOf("remote:POST /api/Tokens", "save:user-a"), fixture.events.log)
        fixture.client.close()
    }

    @Test fun loginAsAnotherUserOrFromGuestClearsLocalDataBeforeSaving() = runBlocking {
        for (previous in listOf(Session("old", "r", "user-b"), null)) {
            val fixture = Fixture(previous) { _, _ -> HttpStatusCode.OK to success(tokenA) }
            fixture.repository.login("a@example.com", "secret1").getOrThrow()
            assertEquals(
                listOf("remote:POST /api/Tokens", "save:null", "favorites:${previous?.userId}", "caches", "save:user-a"),
                fixture.events.log, "previous=${previous?.userId}",
            )
            fixture.client.close()
        }
    }

    @Test fun googleLoginPersistsLikePasswordLogin() = runBlocking {
        val fixture = Fixture(Session("old", "r", "user-b")) { _, _ -> HttpStatusCode.OK to success(tokenA) }
        val session = fixture.repository.googleLogin("google-id-token").getOrThrow()
        assertEquals("user-a", session.userId)
        assertEquals(
            listOf("remote:POST /api/Tokens/google/login", "save:null", "favorites:user-b", "caches", "save:user-a"),
            fixture.events.log,
        )
        fixture.client.close()
    }

    @Test fun aRejectedLoginChangesNothingLocally() = runBlocking {
        val fixture = Fixture(Session("old", "r", "user-b")) { _, _ -> HttpStatusCode.OK to """{"isSuccess":false,"message":"Invalid credentials"}""" }
        val failure = fixture.repository.login("a@example.com", "bad").exceptionOrNull()
        assertEquals("Invalid credentials", failure?.message)
        assertEquals(listOf("remote:POST /api/Tokens"), fixture.events.log)
        assertEquals("old", fixture.sessions.peekSession()?.accessToken)
        fixture.client.close()
    }

    @Test fun logoutCleansLocalDataFirstThenSendsTheRefreshTokenAndSucceeds() = runBlocking {
        val fixture = Fixture(Session("old", "refresh-me", "user-a")) { _, _ -> HttpStatusCode.OK to """{"isSuccess":true}""" }
        assertTrue(fixture.repository.logout().isSuccess)
        assertEquals(
            listOf("save:null", "favorites:user-a", "caches", "remote:DELETE /api/Tokens"),
            fixture.events.log,
        )
        assertTrue(fixture.refreshCookie.orEmpty().contains("refreshToken=refresh-me"))
        assertNull(fixture.sessions.peekSession())
        fixture.client.close()
    }

    @Test fun aFailedRemoteLogoutIsIgnoredBecauseLocalCleanupIsAuthoritative() = runBlocking {
        val fixture = Fixture(Session("old", "refresh-me", "user-a")) { _, _ -> HttpStatusCode.InternalServerError to "{}" }
        assertTrue(fixture.repository.logout().isSuccess)
        assertTrue(fixture.events.log.contains("remote:DELETE /api/Tokens"))
        assertNull(fixture.sessions.peekSession())
        fixture.client.close()
    }

    @Test fun aCleanupFailureIsReportedButTheRemoteLogoutStillRuns() = runBlocking {
        val boom = IllegalStateException("cannot clear favorites")
        val fixture = Fixture(Session("old", "refresh-me", "user-a"), favoritesFailure = boom) { _, _ -> HttpStatusCode.OK to """{"isSuccess":true}""" }
        val result = fixture.repository.logout()
        assertFalse(result.isSuccess)
        assertEquals("cannot clear favorites", result.exceptionOrNull()?.message)
        assertTrue(fixture.events.log.contains("caches"), "Caches are cleared even when favorites fail")
        assertTrue(fixture.events.log.contains("remote:DELETE /api/Tokens"))
        fixture.client.close()
    }

    private class Sessions(initial: Session?, private val events: Events) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = applySession(session)
        override suspend fun saveSession(session: Session?) { events.log += "save:${session?.userId}"; applySession(session) }
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = state.value != null
    }

    private class Favorites(private val events: Events, private val failure: Throwable?) : FavoriteRepository {
        override suspend fun getFavoriteIds(): Set<String> = error("Unused")
        override suspend fun isFavorite(shopId: String): Boolean = error("Unused")
        override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("Unused")
        override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Unused")
        override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Unused")
        override suspend fun clearAll(): Unit = error("Unused")
        override suspend fun clearForUser(userId: String?) {
            events.log += "favorites:$userId"
            failure?.let { throw it }
        }
    }
}
