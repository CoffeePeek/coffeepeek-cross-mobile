package com.coffeepeek.data.repository

import com.coffeepeek.api.service.CheckInApiService
import com.coffeepeek.api.service.PhotoApiService
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class CheckInRepositoryContractTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val visit = """{"id":"visit","shop":null,"author":null,"username":"Анна","shopName":"Кофейня","text":"Обновлённый текст",
        "rating":{"coffee":5,"service":4,"place":4},"createdAtUtc":"2026-10-07T10:00:00Z","visitedAt":"2026-10-07T09:00:00Z",
        "visibility":"Public","moderationState":"Pending","contentRevision":2,"rejectionReason":null,
        "drinkSlug":"cappuccino","drinkNameRu":"Капучино","helpfulCount":7,
        "photos":[{"id":"photo","storageKey":"do-not-invent-url","sortIndex":0,"url":"/api/v1/check-ins/visit/photos/photo"}]}"""

    @Test
    fun createsVisitAfterCheckInPhotoUploadWithSizeBytesAndAttachmentSize() = runBlocking {
        val paths = mutableListOf<String>()
        val client = client(MockEngine { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/Photos/check-in" -> {
                    val target = Json.parseToJsonElement((request.body as TextContent).text).jsonArray.single().jsonObject
                    assertEquals(JsonPrimitive(3), target["sizeBytes"])
                    assertEquals(JsonPrimitive("image/jpeg"), target["contentType"])
                    respond("""{"isSuccess":true,"data":[{"photoId":"upload","uploadUrl":"https://uploads.example/put-photo","storageKey":"check-ins/key.jpg"}]}""", headers = headers)
                }
                "/put-photo" -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("image/jpeg", request.headers[HttpHeaders.ContentType] ?: request.body.contentType?.toString())
                    respond("", HttpStatusCode.OK)
                }
                "/api/v1/check-ins" -> {
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals(JsonPrimitive("coffee-slug"), body["coffeeShopSlug"])
                    assertEquals(JsonPrimitive("Кофе"), body["text"])
                    assertEquals(JsonPrimitive("Private"), body["visibility"])
                    val photo = body["photos"]!!.jsonArray.single().jsonObject
                    assertEquals(JsonPrimitive(3), photo["size"])
                    assertEquals(JsonPrimitive("check-ins/key.jpg"), photo["storageKey"])
                    assertFalse("photoId" in photo)
                    respond("""{"isSuccess":true,"data":$visit}""", headers = headers)
                }
                else -> error("Unexpected route")
            }
        })
        try {
            repository(client).createCheckIn(CreateCheckInInput(
                "coffee-slug", "  Кофе  ", ReviewRating(4, 4, 5),
                photos = listOf(PendingPhotoUpload("coffee.jpg", "image/jpeg", byteArrayOf(1, 2, 3))),
            )).getOrThrow()
            assertEquals(listOf("/api/Photos/check-in", "/put-photo", "/api/v1/check-ins"), paths)
        } finally { client.close() }
    }

    @Test
    fun derivesPagesFromTotalCountAndRetrievesAllCalendarPagesWithinDateRange() = runBlocking {
        val rangePages = mutableListOf<String?>()
        val client = client(MockEngine { request ->
            assertEquals("/api/v1/check-ins/mine", request.url.encodedPath)
            val range = request.url.parameters["from"] != null
            val count = if (range) 2 else 41
            val id = if (request.url.parameters["pageNumber"] == "2") "second" else "visit"
            if (range) {
                rangePages += request.url.parameters["pageNumber"]
                assertEquals("2026-10-01T00:00:00Z", request.url.parameters["from"])
                assertEquals("2026-11-01T00:00:00Z", request.url.parameters["to"])
            }
            respond("""{"isSuccess":true,"data":{"items":[${visit.replace("\"id\":\"visit\"", "\"id\":\"$id\"")}],"totalCount":$count}}""", headers = headers)
        })
        try {
            val repo = repository(client)
            val page = repo.getMyCheckIns(2, 20).getOrThrow()
            assertEquals(3, page.totalPages)
            assertEquals(2, page.currentPage)
            assertEquals(41, page.totalCount)
            val calendar = repo.getMyCheckIns("2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z", 1).getOrThrow()
            assertEquals(listOf<String?>("1", "2"), rangePages)
            assertEquals(listOf("visit", "second"), calendar.map { it.id })
        } finally { client.close() }
    }

    @Test
    fun editingMapsServerStateAndUsesPhotoUrlsWithoutStorageKeyFallbacks() = runBlocking {
        val client = client(MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/v1/check-ins/visit", request.url.encodedPath)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(JsonPrimitive("cappuccino"), body["drinkSlug"])
            assertEquals(JsonPrimitive("Обновлённый текст"), body["text"])
            assertFalse("photos" in body)
            assertFalse("visitedAt" in body)
            respond("""{"isSuccess":true,"statusCode":null,"data":$visit}""", headers = headers)
        })
        try {
            val updated = repository(client).updateCheckIn("visit", UpdateCheckInInput(" Обновлённый текст ", ReviewRating(4, 4, 5), "cappuccino")).getOrThrow()
            assertNull(updated.shopAddress)
            assertNull(updated.authorAddress)
            assertEquals("", updated.shopId)
            assertEquals("Анна", updated.username)
            assertEquals("2026-10-07T10:00:00Z", updated.createdAt)
            assertEquals("2026-10-07T09:00:00Z", updated.visitedAt)
            assertEquals(CheckInModerationState.Pending, updated.moderationState)
            assertEquals(2, updated.contentRevision)
            assertEquals(listOf("https://api.example/api/v1/check-ins/visit/photos/photo"), updated.photoUrls)
            assertEquals(updated.photoUrls, updated.photoThumbnailUrls)
            assertEquals(7, updated.helpfulCount)
        } finally { client.close() }
    }

    @Test
    fun invalidContentDoesNotUploadPhotosOrSpendCreationQuota() = runBlocking {
        var requests = 0
        val client = client(MockEngine { requests++; error("No request expected") })
        try {
            val repo = repository(client)
            val valid = CreateCheckInInput("coffee", "Кофе", ReviewRating(4, 4, 5))
            for (invalid in listOf(
                valid.copy(text = " \n "), valid.copy(text = "x".repeat(1001)),
                valid.copy(rating = ReviewRating(0, 4, 5)),
                valid.copy(drinkSlug = "other", customDrinkName = " "),
                valid.copy(photos = List(6) { PendingPhotoUpload("p.jpg", "image/jpeg", byteArrayOf(1)) }),
            )) assertTrue(repo.createCheckIn(invalid).isFailure)
            assertTrue(repo.updateCheckIn("visit", UpdateCheckInInput(" ", valid.rating)).isFailure)
            assertEquals(0, requests)
        } finally { client.close() }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        defaultRequest { url("https://api.example") }
    }
    private fun repository(client: HttpClient) = CheckInRepositoryImpl(
        CheckInApiService(client), PhotoRepositoryImpl(PhotoApiService(client, client)), FileUrlResolver("https://api.example"),
    )
}
