package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopReviewVoteRepositoryTest {
    @Test fun putAndDeleteReturnServerVoteState() = runBlocking {
        val methods = mutableListOf<String>()
        val engine = MockEngine { request ->
            methods += request.method.value
            assertEquals("/api/CoffeeShopReviews/review-1/helpful", request.url.encodedPath)
            respond("""{"isSuccess":true,"data":{"isHelpful":${request.method.value == "PUT"},"helpfulCount":4}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopReviewVoteRepository(client)
            assertTrue(repository.setHelpful("review-1", true).getOrThrow().isHelpful)
            assertEquals(false, repository.setHelpful("review-1", false).getOrThrow().isHelpful)
            assertEquals(listOf("PUT", "DELETE"), methods)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun rejectedResponseAndUnsafeIdStayResultFailures() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"isSuccess":false,"data":null}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopReviewVoteRepository(client)
            assertTrue(repository.setHelpful("unsafe/id", true).isFailure)
            assertEquals(0, calls)
            assertTrue(repository.setHelpful("review-1", true).isFailure)
            assertEquals(1, calls)
        } finally {
            client.close()
            engine.close()
        }
    }
}
