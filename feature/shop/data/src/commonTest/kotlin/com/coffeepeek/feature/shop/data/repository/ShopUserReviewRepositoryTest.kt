package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopUserReviewRepositoryTest {
    @Test fun findsPublishedReviewOnLaterPageAndKeepsModerationId() = runBlocking {
        val pages = mutableListOf<String?>()
        val engine = MockEngine { request ->
            assertEquals("/api/users/user-1/reviews", request.url.encodedPath)
            assertEquals("100", request.url.parameters["pageSize"])
            pages += request.url.parameters["pageNumber"]
            val reviews = if (pages.size == 1) "[]" else """[{
              "id":"published-1","moderationReviewId":"moderation-1",
              "userId":"user-1","coffeeShopId":"shop-1", "header":"Coffee",
              "comment":"Good filter", "rating":{"place":4,"service":5,"coffee":3},
              "photos":[{"storageKey":"reviews/photo.jpg"}]
            }]"""
            respond("""{"isSuccess":true,"data":{"reviewDtos":$reviews,"totalPages":2}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val review = createShopUserReviewRepository(client, "https://files.example.com")
                .findForEdit("user-1", "published-1").getOrThrow()
            assertEquals(listOf<String?>("1", "2"), pages)
            assertEquals("moderation-1", review?.moderationReviewId)
            assertEquals("shop-1", review?.shopId)
            assertEquals(listOf("https://files.example.com/api/file/reviews/photo.jpg"),
                review?.photoUrls)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun absentReviewAndUnsafeIdsDoNotWriteOrInventOne() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"isSuccess":true,"data":{"reviewDtos":[],"totalPages":1}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopUserReviewRepository(client, "https://files.example.com")
            assertTrue(repository.findForEdit("../user", "review").isFailure)
            assertEquals(0, calls)
            assertNull(repository.findForEdit("user-1", "missing").getOrThrow())
            assertEquals(1, calls)
        } finally {
            client.close()
            engine.close()
        }
    }
}
