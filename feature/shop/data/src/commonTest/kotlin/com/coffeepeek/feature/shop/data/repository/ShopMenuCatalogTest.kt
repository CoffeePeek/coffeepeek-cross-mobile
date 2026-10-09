package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShopMenuCatalogTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val details = """{"isSuccess":true,"data":{"address":{"slug":"shop"},"menu":{"items":[
      {"slug":"unknown-a","nameRu":"Особый"},
      {"slug":"flat-white","nameRu":"Наш флэт","price":6,"availability":"Present"},
      {"slug":"unknown-b"},
      {"slug":"filter"}
    ]}}}"""
    private val catalog = """{"isSuccess":true,"data":{"drinks":[
      {"slug":"flat-white","nameRu":"Флэт уайт","nameEn":"Flat white","category":"Espresso","sortOrder":2},
      {"slug":"filter","nameRu":"Фильтр","nameEn":"Filter","category":"Filter","sortOrder":1}
    ]}}"""

    @Test fun fillsOnlyAbsentLabelsPreservesPricesAndCachesSuccessfulCatalog() = runBlocking {
        var catalogCalls = 0
        var detailCalls = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/menu/drinks" -> { catalogCalls++; respond(catalog, headers = headers) }
                "/api/CoffeeShops/shop" -> { detailCalls++; respond(details, headers = headers) }
                else -> error("Unexpected request")
            }
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopDetailsRepository(client, "https://example.com") { 0 }
            repeat(2) {
                val menu = repository.getDetails("shop").getOrThrow().menu!!
                assertEquals(listOf("filter", "flat-white", "unknown-a", "unknown-b"), menu.items.map { it.slug })
                assertEquals("Фильтр", menu.items.first().nameRu)
                val flatWhite = menu.items[1]
                assertEquals("Наш флэт", flatWhite.nameRu)
                assertEquals("Flat white", flatWhite.nameEn)
                assertEquals("Espresso", flatWhite.category)
                assertEquals(6.0, flatWhite.price)
                assertEquals("Present", flatWhite.availability)
            }
            assertEquals(1, catalogCalls)
            assertEquals(2, detailCalls)
        } finally { client.close(); engine.close() }
    }

    @Test fun catalogHttpEnvelopeAndMalformedFailuresPreserveDetailsAndAreRetried() = runBlocking {
        for ((body, status) in listOf(
            catalog to HttpStatusCode.InternalServerError,
            """{"isSuccess":false,"data":null}""" to HttpStatusCode.OK,
            "malformed" to HttpStatusCode.OK,
        )) {
            var catalogCalls = 0
            val engine = MockEngine { request ->
                if (request.url.encodedPath == "/api/menu/drinks") {
                    catalogCalls++
                    if (catalogCalls == 1) respond(body, status, headers)
                    else respond(catalog, headers = headers)
                } else respond(details, headers = headers)
            }
            val client = HttpClientFactory(engine).api("https://example.com")
            try {
                val repository = createShopDetailsRepository(client, "https://example.com") { 0 }
                val fallback = repository.getDetails("shop").getOrThrow().menu!!
                assertEquals("unknown-a", fallback.items.first().slug)
                assertEquals("", fallback.items.last().nameRu)
                assertEquals("filter", repository.getDetails("shop").getOrThrow().menu!!.items.first().slug)
                assertEquals(2, catalogCalls)
            } finally { client.close(); engine.close() }
        }
    }

    @Test fun optionalCatalogNeverSwallowsCancellation() = runBlocking<Unit> {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/api/menu/drinks") throw CancellationException("cancel")
            respond(details, headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopDetailsRepository(client, "https://example.com") { 0 }
            assertFailsWith<CancellationException> { repository.getDetails("shop") }
        } finally { client.close(); engine.close() }
    }
}
