package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ShopCheckInRepositoryTest {
    @Test fun loadsConsumedDrinksFromCurrentCatalogEndpoint() = runBlocking {
        val apiEngine = MockEngine { request ->
            assertEquals("/api/catalogs/drinks", request.url.encodedPath)
            respond("""{"isSuccess":true,"data":[{"slug":"filter","nameRu":"Фильтр","nameEn":"Filter","category":{"id":1}}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val uploadEngine = MockEngine { error("No upload expected") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val drinks = createShopCheckInRepository(apiClient, uploadClient).getDrinkOptions().getOrThrow()
            assertEquals("filter", drinks.single().slug)
            assertEquals("Фильтр", drinks.single().nameRu)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun privateCheckInUsesCurrentV1FieldsAndRequiresConfirmedCreation() = runBlocking {
        val apiEngine = MockEngine { request ->
            assertEquals("/api/v1/check-ins", request.url.encodedPath)
            assertEquals("POST", request.method.value)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(JsonPrimitive("shop-1"), body["coffeeShopSlug"])
            assertEquals(JsonPrimitive("A visit"), body["text"])
            assertEquals(JsonPrimitive("Private"), body["visibility"])
            assertEquals(JsonPrimitive(4), body["rating"]!!.jsonObject["coffee"])
            assertEquals(emptyList(), body["photos"]!!.jsonArray.toList())
            for (obsolete in listOf("shop", "shopId", "isPublic", "note", "header")) {
                assertFalse(obsolete in body, obsolete)
            }
            respond("""{"isSuccess":true,"data":{"id":"visit-1"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val uploadEngine = MockEngine { error("No upload expected") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            assertTrue(createShopCheckInRepository(apiClient, uploadClient).create(
                validInput()).isSuccess)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun publicCheckInUploadsPhotoBeforeSubmission() = runBlocking {
        val steps = mutableListOf<String>()
        val apiEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/Photos/check-in" -> {
                    steps += "url"
                    respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/photo","storageKey":"check-in/photo"}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                "/api/v1/check-ins" -> {
                    steps += "check-in"
                    val body = (request.body as TextContent).text
                    assertTrue(body.contains("\"storageKey\":\"check-in/photo\""))
                    assertTrue(body.contains("\"drinkSlug\":\"other\""))
                    assertTrue(body.contains("\"customDrinkName\":\"Flat white\""))
                    assertTrue(body.contains("\"text\":\"A good visit\""))
                    assertTrue(body.contains("\"visibility\":\"Public\""))
                    assertTrue(body.contains("\"place\":5"))
                    val photo = Json.parseToJsonElement(body).jsonObject["photos"]!!.jsonArray.single().jsonObject
                    assertEquals(JsonPrimitive(2), photo["size"])
                    assertFalse("sizeBytes" in photo)
                    respond("""{"isSuccess":true,"data":{"id":"visit-1"}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }
        val uploadEngine = MockEngine { request ->
            steps += "upload"
            assertEquals("uploads.example.com", request.url.host)
            assertEquals("PUT", request.method.value)
            assertFalse(request.headers.contains(HttpHeaders.Authorization))
            assertFalse(request.headers.contains("x-amz-tagging"))
            respond("")
        }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val input = ShopCheckInCreateInput(
                shopSlug = "shop-1", visitedAtIso = "2026-10-06T12:00:00Z", visibility = ShopCheckInVisibility.Public,
                drinkSlug = "other", customDrinkName = " Flat white ",
                text = " A good visit ", rating = ShopRating(5, 4, 5),
                photos = listOf(ShopCheckInPhoto(byteArrayOf(1, 2), "visit.jpg")),
            )
            assertTrue(createShopCheckInRepository(apiClient, uploadClient).create(input).isSuccess)
            assertEquals(listOf("url", "upload", "check-in"), steps)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun invalidInputAndUploadUrlNeverSubmitCheckIn() = runBlocking {
        val paths = mutableListOf<String>()
        val apiEngine = MockEngine { request ->
            paths += request.url.encodedPath
            respond("""{"isSuccess":true,"data":[{"uploadUrl":"http://private/photo","storageKey":"x"}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val uploadEngine = MockEngine { error("Upload must not start") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val repository = createShopCheckInRepository(apiClient, uploadClient)
            for (input in listOf(validInput().copy(text = " "),
                validInput().copy(text = "x".repeat(1001)),
                validInput().copy(rating = ShopRating(0, 4, 4)),
                validInput().copy(visitedAtIso = "invalid"))) {
                assertTrue(repository.create(input).isFailure)
            }
            assertTrue(paths.isEmpty())
            assertTrue(repository.create(validInput().copy(
                photos = listOf(ShopCheckInPhoto(byteArrayOf(1), "visit.jpg")))).isFailure)
            assertEquals(listOf("/api/Photos/check-in"), paths)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun unsuccessfulMissingAndMalformedResponsesNeverConfirmCreation() = runBlocking {
        for ((status, body) in listOf(
            HttpStatusCode.Conflict to """{"isSuccess":true,"data":{"id":"visit"}}""",
            HttpStatusCode.OK to """{"isSuccess":false,"message":"Rejected","data":{"id":"visit"}}""",
            HttpStatusCode.OK to "",
            HttpStatusCode.OK to "not-json",
            HttpStatusCode.OK to "{}",
            HttpStatusCode.OK to """{"isSuccess":true,"data":null}""",
            HttpStatusCode.OK to """{"isSuccess":true,"data":{"id":" "}}""",
        )) {
            val apiEngine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
            val uploadEngine = MockEngine { error("No upload expected") }
            val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
            val uploadClient = HttpClientFactory(uploadEngine).upload()
            try {
                val result = createShopCheckInRepository(apiClient, uploadClient).create(validInput())
                assertTrue(result.isFailure, "HTTP ${status.value}: $body")
                if (status == HttpStatusCode.OK && !body.contains("Rejected")) {
                    assertIs<ShopCheckInCreationUnconfirmed>(result.exceptionOrNull())
                }
            } finally {
                apiClient.close(); uploadClient.close(); apiEngine.close(); uploadEngine.close()
            }
        }
    }

    @Test fun serverRejectionMessageIsPreserved() = runBlocking {
        val apiEngine = MockEngine { respond("""{"isSuccess":false,"message":"Creation rejected"}""",
            HttpStatusCode.Forbidden, headersOf(HttpHeaders.ContentType, "application/json")) }
        val uploadEngine = MockEngine { error("No upload expected") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val result = createShopCheckInRepository(apiClient, uploadClient).create(validInput())
            assertEquals("Creation rejected", result.exceptionOrNull()?.message)
        } finally {
            apiClient.close(); uploadClient.close(); apiEngine.close(); uploadEngine.close()
        }
    }

    @Test fun connectionFailureIsUnconfirmedAndCancellationPropagates(): Unit = runBlocking {
        for (error in listOf(IllegalStateException("Disconnected"), CancellationException("Cancelled"))) {
            val apiEngine = MockEngine { throw error }
            val uploadEngine = MockEngine { error("No upload expected") }
            val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
            val uploadClient = HttpClientFactory(uploadEngine).upload()
            try {
                val repository = createShopCheckInRepository(apiClient, uploadClient)
                if (error is CancellationException) {
                    assertFailsWith<CancellationException> { repository.create(validInput()) }
                } else {
                    assertIs<ShopCheckInCreationUnconfirmed>(repository.create(validInput()).exceptionOrNull())
                }
            } finally {
                apiClient.close(); uploadClient.close(); apiEngine.close(); uploadEngine.close()
            }
        }
    }

    private fun validInput() = ShopCheckInCreateInput("shop-1", "A visit", ShopRating(4, 4, 4),
        "2026-10-06T12:00:00Z")
}
