package com.coffeepeek.api.service

import com.coffeepeek.api.model.request.CheckInVisibilityReq
import com.coffeepeek.api.model.request.CreateCheckInReq
import com.coffeepeek.api.model.request.UpdateCheckInReq
import com.coffeepeek.api.model.response.shop.RatingDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class CheckInApiContractTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val visit = """{"id":"visit","text":"Кофе","rating":{"coffee":5,"service":4,"place":4},"shop":null,"author":null,"visibility":"Public","moderationState":"Pending","contentRevision":2}"""

    @Test
    fun historyUsesMineAndQueryPaginationWithBearerAndDateRange() = runBlocking {
        val client = client(MockEngine { request ->
            assertEquals("/api/v1/check-ins/mine", request.url.encodedPath)
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            assertEquals("3", request.url.parameters["pageNumber"])
            assertEquals("10", request.url.parameters["pageSize"])
            assertEquals("2026-10-01T00:00:00Z", request.url.parameters["from"])
            assertEquals("2026-11-01T00:00:00Z", request.url.parameters["to"])
            assertNull(request.headers["X-Page-Number"])
            respond("""{"data":{"items":[$visit],"totalCount":43},"isSuccess":true,"message":"OK","statusCode":null}""", headers = headers)
        })
        try {
            val result = CheckInApiService(client).getMyCheckIns(3, 10, "2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z").getOrThrow()
            assertEquals(43, result.totalCount)
            assertEquals("Pending", result.items.single().moderationState)
        } finally { client.close() }
    }

    @Test
    fun createEditAndVisibilityUseSeparateV1RoutesAndReturnAuthoritativeVisit() = runBlocking {
        var requests = 0
        val client = client(MockEngine { request ->
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            when (requests++) {
                0 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/api/v1/check-ins", request.url.encodedPath)
                    assertEquals(JsonPrimitive("coffee"), body["coffeeShopSlug"])
                }
                1 -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("/api/v1/check-ins/visit", request.url.encodedPath)
                    assertEquals(JsonPrimitive("cappuccino"), body["drinkSlug"])
                    assertFalse("visibility" in body)
                    assertFalse("photos" in body)
                }
                else -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("/api/v1/check-ins/visit/visibility", request.url.encodedPath)
                    assertEquals(setOf("visibility"), body.keys)
                }
            }
            respond("""{"isSuccess":true,"statusCode":null,"data":$visit}""", headers = headers)
        })
        try {
            val api = CheckInApiService(client)
            assertEquals("visit", api.createCheckIn(CreateCheckInReq("coffee", "Кофе", RatingDto(4, 4, 5))).getOrThrow().id)
            assertEquals(2, api.updateCheckIn("visit", UpdateCheckInReq("Кофе", RatingDto(4, 4, 5), "cappuccino")).getOrThrow().contentRevision)
            assertEquals("Pending", api.setVisibility("visit", CheckInVisibilityReq("Public")).getOrThrow().moderationState)
            assertEquals(3, requests)
        } finally { client.close() }
    }

    @Test
    fun helpfulAndReportsUseCanonicalCheckInRoutesWithNoVoteBodyAndTrimmedReport() = runBlocking {
        var requests = 0
        val client = client(MockEngine { request ->
            when (requests++) {
                0, 1 -> {
                    assertEquals("/api/v1/check-ins/visit/helpful", request.url.encodedPath)
                    assertEquals(if (requests == 1) HttpMethod.Put else HttpMethod.Delete, request.method)
                    assertTrue(request.body is io.ktor.http.content.OutgoingContent.NoContent)
                    respond("""{"isSuccess":true,"data":{"isHelpful":${requests == 1},"helpfulCount":8}}""", headers = headers)
                }
                else -> {
                    assertEquals("/api/v1/check-ins/visit/reports", request.url.encodedPath)
                    assertEquals(HttpMethod.Post, request.method)
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals(JsonPrimitive("Reason"), body["text"])
                    respond("""{"isSuccess":true,"data":{"id":"report","checkInId":"visit"}}""", HttpStatusCode.Created, headers)
                }
            }
        })
        try {
            val api = CheckInApiService(client)
            assertTrue(api.setHelpful("visit", true).getOrThrow().isHelpful)
            assertFalse(api.setHelpful("visit", false).getOrThrow().isHelpful)
            api.report("visit", " Reason ").getOrThrow()
            assertEquals(3, requests)
        } finally { client.close() }
    }

    @Test
    fun httpFailureEnvelopeFailureAndMissingDataCannotBecomeSuccess() = runBlocking {
        for ((status, success, data) in listOf(
            Triple(HttpStatusCode.Conflict, true, visit),
            Triple(HttpStatusCode.OK, false, visit),
            Triple(HttpStatusCode.OK, true, "null"),
        )) {
            val client = client(MockEngine { respond("""{"isSuccess":$success,"message":"Error","data":$data}""", status, headers) })
            try {
                val api = CheckInApiService(client)
                assertTrue(api.createCheckIn(CreateCheckInReq("coffee", "Кофе", RatingDto(4, 4, 5))).isFailure)
                assertTrue(api.updateCheckIn("visit", UpdateCheckInReq("Кофе", RatingDto(4, 4, 5))).isFailure)
                assertTrue(api.setVisibility("visit", CheckInVisibilityReq("Public")).isFailure)
            } finally { client.close() }
        }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        defaultRequest { url("https://api.example"); header(HttpHeaders.Authorization, "Bearer test-token") }
    }
}
