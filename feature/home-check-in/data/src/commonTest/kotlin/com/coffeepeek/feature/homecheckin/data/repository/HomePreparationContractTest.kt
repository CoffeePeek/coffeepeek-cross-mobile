package com.coffeepeek.feature.homecheckin.data.repository

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomePreparationContractTest {
    private val id = "11111111-1111-4111-8111-111111111111"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val entry = """{"id":"$id","preparedAtUtc":"2026-09-20T22:15:00Z","preparedOn":"2026-09-21","createdAtUtc":"2026-10-01T08:00:00Z","beanName":"Honey Bloom","coffeeProductId":"22222222-2222-4222-8222-222222222222","doseGrams":75,"tasteScore":4,"photos":[{"id":"photo-1","url":"/api/v1/check-ins/$id/photos/photo-1"}]}"""

    @Test
    fun visibilityChoiceIsSentOnCreateAndEditAndServerStatusIsRead() = runBlocking {
        var calls = 0
        val gateway = gateway(MockEngine { request ->
            val expected = if (calls++ == 0) "Public" else "Private"
            assertEquals(if (calls == 1) HttpMethod.Post else HttpMethod.Put, request.method)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(JsonPrimitive(expected), body["visibility"])
            val status = if (expected == "Public") "Pending" else "NotSubmitted"
            val result = entry.dropLast(1) + """, "visibility":"$expected", "moderationState":"$status"}"""
            respond("""{"isSuccess":true,"data":$result}""", headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { error("No photo upload expected") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            val input = HomePreparationInput("2026-09-21T01:15:00+03:00", visibility = HomePreparationVisibility.Public)
            val published = repository.create(input).getOrThrow()
            assertEquals(HomePreparationVisibility.Public, published.visibility)
            assertEquals(HomePreparationModeration.Pending, published.moderation)
            val hidden = repository.update(id, input.copy(visibility = HomePreparationVisibility.Private)).getOrThrow()
            assertEquals(HomePreparationVisibility.Private, hidden.visibility)
            assertEquals(HomePreparationModeration.NotSubmitted, hidden.moderation)
            assertEquals(2, calls)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun oldServerCannotSilentlyConfirmPublicationAndUnknownVisibilityIsPreservedOnEdit() = runBlocking {
        var calls = 0
        val gateway = gateway(MockEngine { request ->
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            if (calls++ == 0) assertEquals(JsonPrimitive("Public"), body["visibility"])
            else assertFalse("visibility" in body, "An unknown value must not change server visibility")
            respond("""{"isSuccess":true,"data":$entry}""", headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { error("No photo upload expected") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            val input = HomePreparationInput("2026-09-21T01:15:00+03:00", visibility = HomePreparationVisibility.Public)
            val result = repository.create(input).getOrThrow()
            assertNull(result.visibility)
            assertEquals(HomePreparationModeration.Unknown, result.moderation)
            repository.update(id, input.copy(visibility = result.visibility)).getOrThrow()
            assertEquals(2, calls)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun createUploadsPrivatePhotosAndEditPreservesCatalogIdentityWithoutSendingPhotos() = runBlocking {
        var gatewayRequests = 0
        val gateway = gateway(MockEngine { request ->
            when (gatewayRequests++) {
                0 -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/api/Photos/check-in", request.url.encodedPath)
                    respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/signed","storageKey":"owned/key"}]}""", headers = jsonHeaders)
                }
                1 -> {
                    assertEquals("/api/v1/check-ins/home", request.url.encodedPath)
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals(JsonPrimitive("2026-09-21T01:15:00+03:00"), body["preparedAt"])
                    assertEquals(JsonPrimitive("batch_drip"), body["brewMethodCode"])
                    assertEquals(JsonPrimitive("honey-bloom"), body["coffeeProductSlug"])
                    assertEquals(JsonPrimitive(75.0), body["doseGrams"])
                    assertEquals("owned/key", body["photos"]!!.toString().substringAfter("storageKey\":\"").substringBefore('"'))
                    respond("""{"isSuccess":true,"data":$entry}""", headers = jsonHeaders)
                }
                else -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("/api/v1/check-ins/home/$id", request.url.encodedPath)
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals(JsonPrimitive("22222222-2222-4222-8222-222222222222"), body["coffeeProductId"])
                    assertFalse("photos" in body)
                    assertFalse("drinkPhoto" in body)
                    respond("""{"isSuccess":true,"data":$entry}""", headers = jsonHeaders)
                }
            }
        })
        val uploader = HttpClient(MockEngine { request ->
            assertEquals("uploads.example.com", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            assertEquals("image/jpeg", request.headers[HttpHeaders.ContentType] ?: request.body.contentType?.toString())
            assertEquals("is_permanent=False", request.headers["x-amz-tagging"])
            respond("", headers = jsonHeaders)
        })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            val created = repository.create(HomePreparationInput(
                preparedAt = "2026-09-21T01:15:00+03:00", preparedOn = "2026-09-21",
                brewMethodCode = "batch_drip", coffeeProductSlug = "honey-bloom", doseGrams = "75",
                photos = listOf(HomePreparationUpload("brew.jpg", "image/jpeg", byteArrayOf(1, 2))),
            )).getOrThrow()
            assertEquals("https://gateway.example.com/api/v1/check-ins/$id/photos/photo-1", created.photos.single().url)
            repository.update(id, HomePreparationInput(
                preparedAt = created.preparedAtUtc, preparedOn = created.preparedOn,
                coffeeProductId = created.coffeeProductId,
            )).getOrThrow()
            assertEquals(3, gatewayRequests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun ownerHistoryCalendarAndDeleteUseDedicatedRoutes() = runBlocking {
        var calls = 0
        val gateway = gateway(MockEngine { request ->
            when (calls++) {
                0 -> {
                    assertEquals("/api/v1/check-ins/home", request.url.encodedPath)
                    assertEquals("2", request.url.parameters["pageNumber"])
                    assertEquals("100", request.url.parameters["pageSize"])
                    assertEquals("2026-09-01", request.url.parameters["from"])
                    assertEquals("2026-09-30", request.url.parameters["to"])
                    respond("""{"isSuccess":true,"data":{"items":[$entry],"totalCount":101}}""", headers = jsonHeaders)
                }
                1 -> {
                    assertEquals("/api/v1/check-ins/home/calendar", request.url.encodedPath)
                    respond("""{"isSuccess":true,"data":[{"preparedOn":"2026-09-21","brewCount":2}]}""", headers = jsonHeaders)
                }
                2 -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("1", request.url.parameters["pageNumber"])
                    assertEquals("2026-09-01", request.url.parameters["from"])
                    assertEquals("2026-09-30", request.url.parameters["to"])
                    respond("""{"isSuccess":true,"data":{"items":[$entry],"totalCount":1}}""", headers = jsonHeaders)
                }
                else -> {
                    assertEquals(HttpMethod.Delete, request.method)
                    assertEquals("/api/v1/check-ins/home/$id", request.url.encodedPath)
                    respond("""{"isSuccess":true,"data":null}""", headers = jsonHeaders)
                }
            }
        })
        val uploader = HttpClient(MockEngine { respond("") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            assertTrue(repository.page(2, 100, HomePreparationDateRange("2026-09-01", "2026-09-30")).getOrThrow().hasMore.not())
            assertEquals(2, repository.calendar(HomePreparationDateRange("2026-09-01", "2026-09-30")).getOrThrow().single().brewCount)
            repository.delete(id).getOrThrow()
            assertEquals(4, calls)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun originalUploadsNeverReserveOrSendRetiredCutouts() = runBlocking {
        var reservations = 0
        var created = false
        val gateway = gateway(MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/Photos/check-in" -> {
                    reservations++
                    val body = Json.parseToJsonElement((request.body as TextContent).text).toString()
                    assertTrue("image/jpeg" in body)
                    respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/$reservations","storageKey":"key-$reservations"}]}""", headers = jsonHeaders)
                }
                else -> {
                    created = true
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertTrue("key-1" in body["photos"].toString())
                    assertFalse("key-2" in body["photos"].toString())
                    assertFalse("drinkPhoto" in body)
                    respond("""{"isSuccess":true,"data":$entry}""", headers = jsonHeaders)
                }
            }
        })
        var uploads = 0
        val uploader = HttpClient(MockEngine { request ->
            uploads++
            assertEquals("is_permanent=False", request.headers["x-amz-tagging"])
            assertEquals("image/jpeg",
                request.headers[HttpHeaders.ContentType] ?: request.body.contentType?.toString())
            respond("")
        })
        try {
            createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
                .create(HomePreparationInput("2026-09-21T01:15:00+03:00",
                    photos = listOf(HomePreparationUpload("original.jpg", "image/jpeg", byteArrayOf(1))),
                )).getOrThrow()
            assertEquals(1, reservations)
            assertEquals(1, uploads)
            assertTrue(created)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun invalidInputsStopBeforeUpload() {
        val base = HomePreparationInput("2026-09-21T01:15:00+03:00")
        assertNull(validation(base))
        assertTrue(validation(base.copy(preparedAt = "2026-09-21T01:15")) != null)
        assertTrue(validation(base.copy(preparedAt = "2026-09-21T25:15:00+03:00")) != null)
        assertTrue(validation(base.copy(preparedOn = "2026-09-23")) != null)
        assertNull(validation(base.copy(preparedOn = "2026-09-20")))
        assertTrue(validation(base.copy(doseGrams = "18.001")) != null)
        assertTrue(validation(base.copy(coffeeProductSlug = "bean", customBeanName = "Other")) != null)
        assertTrue(validation(base.copy(tasteScore = 6)) != null)
        assertNull(validation(base.copy(drinkSlug = "other", customDrinkName = "Мой кофе")))
        assertTrue(validation(base.copy(customDrinkName = "Мой кофе")) != null)
        assertTrue(validation(base.copy(drinkSlug = "espresso", customDrinkName = "Мой кофе")) != null)
        assertTrue(validation(base.copy(drinkSlug = "other", customDrinkName = " ")) != null)
        assertTrue(validation(base.copy(drinkSlug = "other", customDrinkName = "а".repeat(101))) != null)
        assertTrue(validation(base.copy(photos = List(6) { HomePreparationUpload("x", "image/jpeg", byteArrayOf(1)) })) != null)
    }

    @Test
    fun catalogMethodAndCustomDrinkAreSentOnCreateAndEdit() = runBlocking {
        var requests = 0
        val gateway = gateway(MockEngine { request ->
            assertEquals(if (requests++ == 0) HttpMethod.Post else HttpMethod.Put, request.method)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(JsonPrimitive("cezve-turkish-coffee"), body["brewMethodCode"])
            assertEquals(JsonPrimitive("other"), body["drinkSlug"])
            assertEquals(JsonPrimitive("Мой кофе"), body["customDrinkName"])
            assertEquals(JsonPrimitive(195), body["brewDurationSeconds"])
            respond("""{"isSuccess":true,"data":$entry}""", headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { error("No photo upload expected") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            val input = HomePreparationInput("2026-10-13T08:00:00Z", brewMethodCode = "cezve-turkish-coffee",
                drinkSlug = "other", customDrinkName = "  Мой кофе  ", brewDurationSeconds = 195)
            repository.create(input).getOrThrow()
            repository.update(id, input).getOrThrow()
            assertEquals(2, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test
    fun privateDetailFailureNeverBecomesSuccess() = runBlocking {
        val gateway = gateway(MockEngine { request ->
            assertEquals("/api/v1/check-ins/home/$id", request.url.encodedPath)
            respond("""{"isSuccess":false,"message":"Запись не найдена","data":null}""",
                status = HttpStatusCode.NotFound, headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { respond("") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            assertEquals(HomePreparationIssue.NotFound, (repository.detail(id).exceptionOrNull() as HomePreparationFailure).issue)
        } finally { gateway.close(); uploader.close() }
    }

    private fun validation(input: HomePreparationInput) = homePreparationFieldErrors(input).values.firstOrNull()

    private fun gateway(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        defaultRequest {
            url("https://gateway.example.com")
            headers.append(HttpHeaders.Authorization, "Bearer fixture")
            headers.append(HttpHeaders.Cookie, "session=fixture")
        }
    }
}
