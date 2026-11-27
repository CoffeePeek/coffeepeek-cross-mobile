@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.test.*

class UsernameSelectionRepositoryTest {
    @Test fun missingFlagRemainsCompatibleWhenClaimsMatch() = runTest {
        val fixture = Fixture()
        fixture.flag = null
        assertFalse(fixture.repository.load(fixture.owner).getOrThrow().required)
        assertEquals(0, fixture.refreshes)
        fixture.client.close()
    }

    @Test fun completedSaveRefreshesNameClaimsBeforeReturningReady() = runTest {
        val fixture = Fixture()
        fixture.flag = true
        val result = fixture.repository.complete(fixture.owner, "  Новый_ник  ").getOrThrow()
        assertFalse(result.required)
        assertEquals("Новый_ник", result.username)
        assertEquals(1, fixture.refreshes)
        assertTrue(fixture.lastBody.contains("Новый_ник"))
        fixture.client.close()
    }

    @Test fun restartAfterCommittedSaveCannotUseStaleClaims() = runTest {
        val fixture = Fixture()
        fixture.name = "new_name"
        fixture.allowRefresh = false
        assertTrue(fixture.repository.load(fixture.owner).isFailure)
        fixture.allowRefresh = true
        assertFalse(fixture.repository.load(fixture.owner).getOrThrow().required)
        assertEquals(2, fixture.refreshes)
        fixture.client.close()
    }

    @Test fun takenAndAlreadyCompletedAreDifferentFailures() = runTest {
        val fixture = Fixture()
        fixture.errorCode = "USERNAME_TAKEN"
        val taken = fixture.repository.complete(fixture.owner, "chosen").exceptionOrNull() as UsernameSelectionException
        assertEquals(UsernameSelectionFailure.Taken, taken.reason)
        fixture.errorCode = "USERNAME_SELECTION_ALREADY_COMPLETED"
        val conflict = fixture.repository.complete(fixture.owner, "chosen").exceptionOrNull() as UsernameSelectionException
        assertEquals(UsernameSelectionFailure.Conflict, conflict.reason)
        fixture.client.close()
    }

    @Test fun responseFromReplacedLoginIsCancelledEvenForSameUserId() = runTest {
        val fixture = Fixture()
        fixture.afterRead = { fixture.sessions.generation++ }
        assertFailsWith<CancellationException> { fixture.repository.load(fixture.owner) }
        fixture.client.close()
    }

    private class Fixture {
        val owner = UsernameSelectionOwner("A", 1)
        val sessions = Sessions()
        var name = "chosen"
        var flag: Boolean? = false
        var errorCode: String? = null
        var lastBody = ""
        var refreshes = 0
        var allowRefresh = true
        var afterRead: () -> Unit = {}
        val client = HttpClient(MockEngine { request ->
            if (request.method == HttpMethod.Put) {
                lastBody = request.body.toByteArray().decodeToString()
                if (errorCode != null) return@MockEngine respond("""{"isSuccess":false,"errorCode":"$errorCode"}""",
                    HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/json"))
                name = Json.parseToJsonElement(lastBody).let {
                    (it as kotlinx.serialization.json.JsonObject)["username"].toString().trim('"')
                }
                flag = false
            } else afterRead()
            val required = flag?.let { ",\"usernameSelectionRequired\":$it" }.orEmpty()
            respond("""{"isSuccess":true,"data":{"userName":"$name"$required}}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest { url("https://example.invalid"); header(HttpHeaders.Authorization, "Bearer ${sessions.session.value!!.accessToken}") }
        }
        val repository = createUsernameSelectionRepository(client, sessions, refresh = {
            refreshes++
            if (allowRefresh) sessions.session.value = Session(token(name), "refresh", "A")
            allowRefresh
        })
    }

    private class Sessions : SessionReader {
        var generation = 1L
        val session = MutableStateFlow<Session?>(Session(token("chosen"), "refresh", "A"))
        override fun accountGeneration() = generation
        override fun peekSession() = session.value
        override fun isActiveSession(session: Session?) = session != null
        override fun observeSession() = session
        override suspend fun getSession() = session.value
    }
    companion object {
        private fun token(name: String) = "e30.${Base64.UrlSafe.encode("""{"name":"$name"}""".encodeToByteArray())}.signature"
    }
}
