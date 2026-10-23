package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.auth.api.LocalUserData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The new repository must do what the legacy one did: same requests, same persistence order, same sign-out rules. */
class AuthRepositoryImplTest {
    // JWT payloads {"sub":"user-a"} and {"sub":"user-b"}
    private val tokenA = "header.eyJzdWIiOiJ1c2VyLWEifQ.signature"
    private fun success(token: String, refresh: String = "refresh") = """{"isSuccess":true,"data":{"accessToken":"$token","refreshToken":"$refresh"}}"""

    private class Recorded(val method: HttpMethod, val path: String, val query: String, val body: String, val cookie: String?)

    private inner class Fixture(
        previous: Session?,
        localFailure: Throwable? = null,
        private val answer: (HttpMethod, String) -> Triple<HttpStatusCode, String, Map<String, String>>,
    ) {
        val log = mutableListOf<String>()
        val requests = mutableListOf<Recorded>()
        val store = Store(previous, log)
        val local = Local(log, localFailure)
        private fun engine(client: String) = MockEngine { request: HttpRequestData ->
            log += "remote:$client:${request.method.value} ${request.url.encodedPath}"
            requests += Recorded(request.method, request.url.encodedPath, request.url.encodedQuery, request.body.toByteArray().decodeToString(), request.headers[HttpHeaders.Cookie])
            val (status, body, extra) = answer(request.method, request.url.encodedPath)
            val headers = io.ktor.http.HeadersBuilder().apply {
                append(HttpHeaders.ContentType, "application/json")
                extra.forEach { (k, v) -> append(k, v) }
            }.build()
            respond(body, status, headers)
        }
        private fun client(name: String) = HttpClient(engine(name)) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val plain = client("plain")
        val authenticated = client("auth")
        val repository: com.coffeepeek.feature.auth.domain.AuthRepository = AuthRepositoryImpl(plain, authenticated, store, local)
        fun close() { plain.close(); authenticated.close() }
    }

    private fun ok(body: String, extra: Map<String, String> = emptyMap()) = Triple(HttpStatusCode.OK, body, extra)

    @Test fun signInSendsTheNormalizedEmailAndSavesTheSessionWithTheTokenUserId() = runBlocking {
        val f = Fixture(Session("old", "r", "user-a")) { _, _ -> ok(success(tokenA)) }
        val account = f.repository.signIn(" Person@Example.COM ", "  secret1 ").getOrThrow()
        assertEquals("user-a", account.userId)
        assertEquals(listOf("remote:plain:POST /api/Tokens", "save:user-a"), f.log, "Same user: nothing is cleared")
        val request = f.requests.single()
        assertEquals("""{"email":"person@example.com","password":"  secret1 "}""", request.body)
        assertEquals(Session(tokenA, "refresh", "user-a"), f.store.peekSession())
        f.close()
    }

    @Test fun anotherUserOrAGuestHasLocalDataClearedBeforeTheNewSessionIsSaved() = runBlocking {
        for (previous in listOf(Session("old", "r", "user-b"), null)) {
            val f = Fixture(previous) { _, _ -> ok(success(tokenA)) }
            f.repository.signIn("a@example.com", "secret1").getOrThrow()
            assertEquals(listOf("remote:plain:POST /api/Tokens", "clear", "save:user-a"), f.log, "previous=${previous?.userId}")
            f.close()
        }
    }

    @Test fun googleSignInUsesItsEndpointAndPersistsTheSameWay() = runBlocking {
        val f = Fixture(Session("old", "r", "user-b")) { _, _ -> ok(success(tokenA)) }
        f.repository.signInWithGoogle("google-id-token").getOrThrow()
        assertEquals(listOf("remote:plain:POST /api/Tokens/google/login", "clear", "save:user-a"), f.log)
        assertEquals("""{"idToken":"google-id-token"}""", f.requests.single().body)
        f.close()
    }

    @Test fun theRefreshTokenCookieWinsOverTheBodyAndIsPercentDecoded() = runBlocking {
        val f = Fixture(null) { _, _ -> ok(success(tokenA, refresh = "from-body"), mapOf("Set-Cookie" to "refreshToken=from%2Dcookie; Path=/; HttpOnly")) }
        f.repository.signIn("a@example.com", "secret1").getOrThrow()
        assertEquals("from-cookie", f.store.peekSession()?.refreshToken)
        f.close()
    }

    @Test fun aRejectedSignInKeepsTheServerMessageAndChangesNothingLocally() = runBlocking {
        val f = Fixture(Session("old", "r", "user-b")) { _, _ -> ok("""{"isSuccess":false,"message":"Invalid credentials"}""") }
        val error = f.repository.signIn("a@example.com", "bad").exceptionOrNull()
        assertEquals("Invalid credentials", error?.message)
        assertEquals(listOf("remote:plain:POST /api/Tokens"), f.log)
        assertEquals("old", f.store.peekSession()?.accessToken)
        val blank = Fixture(null) { _, _ -> ok("""{"isSuccess":false}""") }
        assertEquals("Сервер вернул некорректный ответ", blank.repository.signIn("a@example.com", "bad").exceptionOrNull()?.message)
        f.close(); blank.close()
    }

    @Test fun registrationNormalizesTheEmailAndReportsARejection() = runBlocking {
        val f = Fixture(null) { _, _ -> ok("""{"isSuccess":true,"data":null}""") }
        assertTrue(f.repository.register("Coffee User", " USER@Example.com ", "password1").isSuccess)
        assertEquals("""{"email":"user@example.com","password":"password1","userName":"Coffee User"}""", f.requests.single().body)
        val rejected = Fixture(null) { _, _ -> ok("""{"isSuccess":false,"message":"Email already registered"}""") }
        assertEquals("Email already registered", rejected.repository.register("Coffee User", "user@example.com", "password1").exceptionOrNull()?.message)
        f.close(); rejected.close()
    }

    @Test fun theEmailCheckTreats404AsFreeAndOtherwiseReadsTheEnvelope() = runBlocking {
        val taken = Fixture(null) { _, _ -> ok("""{"isSuccess":true,"data":true}""") }
        assertEquals(true, taken.repository.isEmailTaken(" Taken@Example.com ").getOrThrow())
        assertEquals("email=taken%40example.com", taken.requests.single().query)
        val free = Fixture(null) { _, _ -> ok("""{"isSuccess":true,"data":false}""") }
        assertEquals(false, free.repository.isEmailTaken("free@example.com").getOrThrow())
        val missing = Fixture(null) { _, _ -> Triple(HttpStatusCode.NotFound, "{}", emptyMap()) }
        assertEquals(false, missing.repository.isEmailTaken("free@example.com").getOrThrow())
        val failed = Fixture(null) { _, _ -> ok("""{"isSuccess":false,"data":true}""") }
        assertEquals(false, failed.repository.isEmailTaken("x@example.com").getOrThrow())
        taken.close(); free.close(); missing.close(); failed.close()
    }

    @Test fun passwordResetNormalizesTheEmailAndFailsOnHttpOrApplicationErrors() = runBlocking {
        val f = Fixture(null) { _, _ -> ok("""{"isSuccess":true}""") }
        assertTrue(f.repository.requestPasswordReset(" USER@Example.com ").isSuccess)
        assertEquals("/api/Users/password/forgot", f.requests.single().path)
        assertEquals("""{"email":"user@example.com"}""", f.requests.single().body)
        val http = Fixture(null) { _, _ -> Triple(HttpStatusCode.ServiceUnavailable, "{}", emptyMap()) }
        assertTrue(http.repository.requestPasswordReset("user@example.com").isFailure)
        val rejected = Fixture(null) { _, _ -> ok("""{"isSuccess":false}""") }
        assertTrue(rejected.repository.requestPasswordReset("user@example.com").isFailure)
        f.close(); http.close(); rejected.close()
    }

    @Test fun signOutCleansLocalDataFirstThenSendsTheRefreshTokenAndSucceeds() = runBlocking {
        val f = Fixture(Session("old", "refresh-me", "user-a")) { _, _ -> ok("""{"isSuccess":true}""") }
        assertTrue(f.repository.signOut().isSuccess)
        assertEquals(listOf("clear", "remote:auth:DELETE /api/Tokens"), f.log)
        assertTrue(f.requests.single().cookie.orEmpty().contains("refreshToken=refresh-me"))
        f.close()
    }

    @Test fun aFailedServerSignOutIsIgnoredBecauseLocalCleanupIsAuthoritative() = runBlocking {
        val f = Fixture(Session("old", "refresh-me", "user-a")) { _, _ -> Triple(HttpStatusCode.InternalServerError, "{}", emptyMap()) }
        assertTrue(f.repository.signOut().isSuccess)
        assertTrue(f.log.contains("remote:auth:DELETE /api/Tokens"))
        f.close()
    }

    @Test fun aCleanupFailureIsReportedButTheServerSignOutStillRuns() = runBlocking {
        val f = Fixture(Session("old", "refresh-me", "user-a"), IllegalStateException("cannot clear favorites")) { _, _ -> ok("""{"isSuccess":true}""") }
        val result = f.repository.signOut()
        assertFalse(result.isSuccess)
        assertEquals("cannot clear favorites", result.exceptionOrNull()?.message)
        assertTrue(f.log.contains("remote:auth:DELETE /api/Tokens"))
        f.close()
    }

    @Test fun signOutWithoutASessionSendsNoCookie() = runBlocking {
        val f = Fixture(null) { _, _ -> ok("""{"isSuccess":true}""") }
        assertTrue(f.repository.signOut().isSuccess)
        assertNull(f.requests.single().cookie)
        f.close()
    }

    private class Store(initial: Session?, private val log: MutableList<String>) : SessionStore {
        private val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null
        override fun observeSession(): Flow<Session?> = state
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = applySession(session)
        override suspend fun saveSession(session: Session?) { log += "save:${session?.userId}"; applySession(session) }
        override suspend fun warmCache() = Unit
        override suspend fun isLoggedIn() = state.value != null
    }

    private class Local(private val log: MutableList<String>, private val failure: Throwable?) : LocalUserData {
        override suspend fun clear() { log += "clear"; failure?.let { throw it } }
    }
}
