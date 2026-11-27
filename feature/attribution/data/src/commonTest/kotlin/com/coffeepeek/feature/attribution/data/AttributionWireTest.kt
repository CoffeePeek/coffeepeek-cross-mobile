package com.coffeepeek.feature.attribution.data

import com.coffeepeek.core.session.*
import com.coffeepeek.feature.attribution.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class AttributionWireTest {
    @Test fun strictUnionOmitsUnselectedFieldsAndInternalAccount() {
        val link = attributionEntry(AttributionEntry.Link("abcdefghijklmnop"))
        assertEquals(setOf("kind", "code"), link.keys)
        val unknown = attributionEntry(AttributionEntry.Unknown)
        assertEquals(setOf("kind", "landingPath"), unknown.keys)
        val context = attributionContext(identity())
        assertEquals("account_preference", context.text("permissionBasis"))
        assertEquals("recipient", context.text("sdkDistinctId"))
        assertFalse("accountId" in context)
        assertFalse("userId" in context)
    }

    @Test fun sharesOnlySupportedDestinationsAndOwnDomain() {
        assertEquals("coffee_shop", attributionDestination("/coffee-shops/test").text("entityType"))
        assertEquals("coffees", attributionDestination("/coffees").text("catalog"))
        assertFailsWith<IllegalArgumentException> { attributionDestination("/collections/private") }
        assertFailsWith<IllegalArgumentException> { validatedShareUrl("https://coffeepeek.by.attacker.example/l/abcdefghijklmnop") }
        assertFailsWith<IllegalArgumentException> { validatedShareUrl("https://coffeepeek.by/l/abcdefghijklmnop?secret=x") }
    }

    @Test fun referrerReadsOnlyOneBoundedHandoffToken() {
        assertEquals("token-value", handoffFromReferrer("utm_source=private&cp_handoff=token%2Dvalue"))
        assertNull(handoffFromReferrer("utm_source=google"))
        assertNull(handoffFromReferrer("cp_handoff=a&cp_handoff=b"))
        assertNull(handoffFromReferrer("cp_handoff=%0Aprivate"))
        assertNull(handoffFromReferrer("cp_handoff=" + "x".repeat(4097)))
    }

    @Test fun storageRoundTripAndCorruptionFailClosed() {
        val state = AttributionLocalState(AttributionEvidence("observer", 100, "receipt", 200, "recipient"), "installation", true,
            true, listOf(ObserverRevocation("revoke", 300)))
        assertEquals(state, decodeAttribution(encodeAttribution(state)))
        assertEquals(AttributionLocalState(), decodeAttribution("broken json"))
        assertEquals(AttributionLocalState(), decodeAttribution("{\"version\":99}"))
    }

    @Test fun revokeAndResolveNeverAttachBearerOrCookies() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            if (request.method == HttpMethod.Delete) {
                assertEquals("observer", request.headers["X-Attribution-Observer"])
                respond("", HttpStatusCode.NoContent)
            } else {
                assertNotNull(request.headers["Idempotency-Key"])
                respond("""{"data":{"canonicalPath":"/","trackingEnabled":false}}""", headers = jsonHeaders())
            }
        })
        val backend = AttributionHttpBackend(client, TestSessions())
        backend.revoke("observer")
        assertEquals("/", backend.resolve("abcdefghijklmnop").canonicalPath)
        client.close()
    }

    @Test fun telemetryUsesPrincipalAndSdkIdentityAndLateInstallationIsNotFirstRun() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("Bearer fake-access", request.headers[HttpHeaders.Authorization])
            assertEquals("stable-key", request.headers["Idempotency-Key"])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(false, body["firstRunObserved"]?.jsonPrimitive?.boolean)
            assertEquals("unknown", body.text("installChannel"))
            assertEquals("recipient", body["context"]?.jsonObject?.text("sdkDistinctId"))
            assertFalse("accountId" in body)
            respond("""{"data":{}}""", headers = jsonHeaders())
        })
        val backend = AttributionHttpBackend(client, TestSessions())
        backend.installation("installation", "unknown", AttributionEvidence("observer", 100, "receipt", 200, "recipient"), identity(), "stable-key")
        client.close()
    }

    @Test fun missingPermissionAndAccountMismatchStopBeforeNetwork() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; error("Must not send") })
        val backend = AttributionHttpBackend(client, TestSessions())
        assertFailsWith<AttributionFailure> { backend.touch(AttributionObservation("id", AttributionEntry.Unknown, identity().copy(isCurrent = { false })), null) }
        assertFailsWith<AttributionFailure> { backend.touch(AttributionObservation("id", AttributionEntry.Unknown, identity().copy(accountId = "sender")), null) }
        assertEquals(0, requests)
        client.close()
    }
}

private fun identity() = AttributionIdentity("recipient", "recipient", "session", "android", "2.0.0", "development") { true }
private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private class TestSessions : SessionReader {
    private val current = Session("fake-access", userId = "recipient")
    override fun peekSession() = current
    override fun isActiveSession(session: Session?) = session == current
    override fun observeSession() = flowOf(current)
    override suspend fun getSession() = current
}
