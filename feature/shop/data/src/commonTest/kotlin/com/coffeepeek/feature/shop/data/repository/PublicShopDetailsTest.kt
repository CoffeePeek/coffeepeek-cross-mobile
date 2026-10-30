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
            respond("""{"isSuccess":true,"data":{"address":{"slug":"cafe","canonicalPath":"/coffee-shop/cafe","revision":3,"isAlias":true},"name":"Cafe","rating":4.7,"checkInCount":12,"isOpen":true,
                "priceRange":"2","location":{"address":"Main 1","latitude":53.9,"longitude":27.56},
                "brewMethods":[{"name":"V60"},{"name":" "}],"roasters":[{"photoUrl":"logo"},{"photoUrl":"logo"},{"photoUrl":""}],
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
            assertEquals(Triple("cafe", "/coffee-shop/cafe", 3), Triple(row.slug, row.canonicalPath, row.revision))
            assertEquals(true, row.isAlias)
            assertEquals("Cafe", row.title); assertEquals("$$", row.priceRange)
            assertEquals(53.9 to 27.56, row.latitude to row.longitude); assertEquals("Main 1", row.address)
            assertEquals(listOf("V60"), row.brewMethods); assertEquals(listOf("logo"), row.roasterPhotoUrls)
        } finally { client.close() }
    }
}
