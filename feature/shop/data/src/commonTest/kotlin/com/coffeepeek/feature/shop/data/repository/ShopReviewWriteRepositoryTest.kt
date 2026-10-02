package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput
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

class ShopReviewWriteRepositoryTest {
    private val rating = ShopRating(place = 5, service = 4, coffee = 5)
    private val photo = ShopReviewPhoto(byteArrayOf(1, 2, 3), "coffee.jpg")

    @Test fun createUploadsShopPhotoBeforeSendingModerationReview() = runBlocking {
        val steps = mutableListOf<String>()
        val apiEngine = MockEngine { request ->
            val body = (request.body as TextContent).text
            when (request.url.encodedPath) {
                "/api/Photos/shop" -> {
                    steps += "url"
                    assertEquals("POST", request.method.value)
                    assertTrue(body.contains("\"sizeBytes\":3"))
                    respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/coffee","storageKey":"shop/coffee"}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                "/api/ModerationReviews" -> {
                    steps += "review"
                    assertEquals("POST", request.method.value)
                    assertTrue(body.contains("\"storageKey\":\"shop/coffee\""))
                    assertTrue(body.contains("\"header\":\"Coffee\""))
                    respond("""{"isSuccess":true,"data":{"entityId":"new-1"}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> error("Unexpected API path: ${request.url.encodedPath}")
            }
        }
        val uploadEngine = MockEngine { request ->
            steps += "upload"
            assertEquals("PUT", request.method.value)
            assertEquals("uploads.example.com", request.url.host)
            assertEquals("image/jpeg", request.headers[HttpHeaders.ContentType]
                ?: request.body.contentType?.toString())
            assertFalse(request.headers.contains(HttpHeaders.Authorization))
            respond("")
        }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val repository = createShopReviewWriteRepository(apiClient, uploadClient)
            assertTrue(repository.create(ShopReviewCreateInput("shop-1", " Coffee ",
                " Delicious coffee ", rating, listOf(photo))).isSuccess)
            assertEquals(listOf("url", "upload", "review"), steps)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun updatePreservesCurrentTextOnlyServerCommandWithoutUploadingSelectedPhotos() = runBlocking {
        var apiCalls = 0
        var uploadCalls = 0
        val apiEngine = MockEngine { request ->
            apiCalls++
            assertEquals("PUT", request.method.value)
            assertEquals("/api/ModerationReviews/moderation-1", request.url.encodedPath)
            val body = (request.body as TextContent).text
            assertTrue(body.contains("\"reviewId\":\"moderation-1\""))
            assertFalse(body.contains("photos"))
            respond("""{"isSuccess":true}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val uploadEngine = MockEngine { uploadCalls++; respond("") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val repository = createShopReviewWriteRepository(apiClient, uploadClient)
            assertTrue(repository.update(ShopReviewUpdateInput("moderation-1", "Coffee",
                "Delicious coffee", rating, listOf(photo))).isSuccess)
            assertEquals(1, apiCalls)
            assertEquals(0, uploadCalls)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }

    @Test fun invalidInputAndPrivateUploadUrlFailWithoutWritingReview() = runBlocking {
        val paths = mutableListOf<String>()
        val apiEngine = MockEngine { request ->
            paths += request.url.encodedPath
            respond("""{"isSuccess":true,"data":[{"uploadUrl":"http://minio/photo","storageKey":"x"}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        var uploadCalls = 0
        val uploadEngine = MockEngine { uploadCalls++; respond("") }
        val apiClient = HttpClientFactory(apiEngine).api("https://example.com")
        val uploadClient = HttpClientFactory(uploadEngine).upload()
        try {
            val repository = createShopReviewWriteRepository(apiClient, uploadClient)
            assertTrue(repository.create(ShopReviewCreateInput("shop-1", "x",
                "short", rating, listOf(photo))).isFailure)
            assertTrue(paths.isEmpty())
            assertTrue(repository.create(ShopReviewCreateInput("shop-1", "Coffee",
                "Delicious coffee", rating, listOf(photo))).isFailure)
            assertEquals(listOf("/api/Photos/shop"), paths)
            assertEquals(0, uploadCalls)
        } finally {
            apiClient.close()
            uploadClient.close()
            apiEngine.close()
            uploadEngine.close()
        }
    }
}
