package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicShopDetailsTest {
    @Test fun summaryKeepsCardCoverAndLegacyTagOrderWithoutLoadingMenuCatalog(): Unit = runBlocking {
        var calls = 0
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/CoffeeShops/cafe", request.url.encodedPath)
            calls++
            respond("""{"isSuccess":true,"data":{"address":{"slug":"cafe"},"name":"Cafe","rating":4.7,"checkInCount":12,"isOpen":true,
                "photos":[{"fullUrl":"full","urls":{"card":"card","detail":"detail"}}],
                "tags":[" Фильтр ",{"name":"Зерно"}],"shopTags":[{"title":"Фильтр"},{"label":"Еда"},"Четвёртый"],
                "menu":{"items":[{"slug":"coffee"}]}}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }).api("https://api.example")
        try {
            val row = createPublicShopDetails(client).load("cafe").getOrThrow()
            assertEquals("card", row.photoUrl)
            assertEquals(listOf("Фильтр", "Зерно", "Еда"), row.tags)
            assertEquals(4.7, row.rating)
            assertEquals(12, row.reviewCount)
            assertEquals(1, calls)
        } finally { client.close() }
    }
}
