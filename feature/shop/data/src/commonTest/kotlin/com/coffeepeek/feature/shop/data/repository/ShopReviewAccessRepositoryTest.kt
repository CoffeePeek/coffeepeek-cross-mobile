package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shop.domain.model.ShopReviewAccess
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopReviewAccessRepositoryTest {
    @Test fun mapsEligibilityAndExistingReviewFromAuthenticatedEndpoint() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/CoffeeShopReviews/can-create", request.url.encodedPath)
            assertEquals("shop-1", request.url.parameters["shopId"])
            respond("""{"isSuccess":true,"data":{"canCreate":false,"reviewId":"review-1"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            assertEquals(ShopReviewAccess(false, "review-1"),
                createShopReviewAccessRepository(client).getAccess("shop-1").getOrThrow())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun rejectionAndBlankIdRemainResultFailures() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"isSuccess":false,"data":null}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopReviewAccessRepository(client)
            assertTrue(repository.getAccess("").isFailure)
            assertEquals(0, calls)
            assertTrue(repository.getAccess("shop-1").isFailure)
            assertEquals(1, calls)
        } finally {
            client.close()
            engine.close()
        }
    }
}
