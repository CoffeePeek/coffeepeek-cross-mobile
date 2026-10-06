package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopRating
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test fun privateCheckInKeepsOptionalFieldsAbsent() = runBlocking {
        val apiEngine = MockEngine { request ->
            assertEquals("/api/CheckIns", request.url.encodedPath)
            val body = (request.body as TextContent).text
            assertTrue(body.contains("\"shop\":\"shop-1\""))
            assertTrue(body.contains("\"isPublic\":false"))
            assertFalse(body.contains("\"header\""))
            assertFalse(body.contains("\"rating\""))
            assertFalse(body.contains("\"photos\""))
            respond("", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val uploadEngine = MockEngine { error("No upload expected") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            assertTrue(createShopCheckInRepository(apiClient, uploadClient).create(
                ShopCheckInCreateInput("shop-1", "2026-10-06T12:00:00Z", false)).isSuccess)
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
                "/api/Photos/shop" -> {
                    steps += "url"
                    respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/photo","storageKey":"shop/photo"}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                "/api/CheckIns" -> {
                    steps += "check-in"
                    val body = (request.body as TextContent).text
                    assertTrue(body.contains("\"storageKey\":\"shop/photo\""))
                    assertTrue(body.contains("\"drinkSlug\":\"other\""))
                    assertTrue(body.contains("\"customDrinkName\":\"Flat white\""))
                    assertTrue(body.contains("\"header\":\"Coffee\""))
                    assertTrue(body.contains("\"place\":5"))
                    respond("""{"isSuccess":true,"data":{"id":"visit-1"}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }
        val uploadEngine = MockEngine { request ->
            steps += "upload"
            assertEquals("uploads.example.com", request.url.host)
            assertFalse(request.headers.contains(HttpHeaders.Authorization))
            respond("")
        }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val input = ShopCheckInCreateInput(
                shopId = "shop-1", visitedAtIso = "2026-10-06T12:00:00Z", isPublic = true,
                drinkSlug = "other", customDrinkName = " Flat white ", header = " Coffee ",
                note = " A good visit ", rating = ShopRating(5, 4, 5),
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
            assertTrue(repository.create(ShopCheckInCreateInput("shop-1", "2026-10-06T12:00:00Z",
                true)).isFailure)
            assertTrue(paths.isEmpty())
            assertTrue(repository.create(ShopCheckInCreateInput("shop-1", "2026-10-06T12:00:00Z",
                false, photos = listOf(ShopCheckInPhoto(byteArrayOf(1), "visit.jpg")))).isFailure)
            assertEquals(listOf("/api/Photos/shop"), paths)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }
}
