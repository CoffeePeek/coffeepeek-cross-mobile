package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShopMenuGalleryRepositoryTest {
    @Test fun loadsSortedMenuPhotosFromShopDetails() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https://example.com/api/CoffeeShops/shop-1", request.url.toString())
            respond("""{
                "isSuccess":true,
                "data":{
                    "shopDto":{
                        "name":"Кофейня",
                        "unusedField":9,
                        "menu":{"photos":[
                            {"id":"later","fullUrl":"https://photo/later","sortIndex":2},
                            {"id":"first","fullUrl":"https://photo/original","urls":{"fullscreen":"https://photo/full","detail":"https://photo/detail"},"sortIndex":1},
                            {"id":"missing","sortIndex":3}
                        ]}
                    }
                }
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val gallery = createShopMenuGalleryRepository(client).getMenuGallery("shop-1").getOrThrow()
            assertEquals("Кофейня", gallery.shopTitle)
            assertEquals(listOf("first", "later"), gallery.photos.map { it.id })
            assertEquals("https://photo/full", gallery.photos.first().fullUrl)
            assertEquals("https://photo/detail", gallery.photos.first().previewUrl)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun fallsBackToTopLevelMenuAndRejectsFailedEnvelope() = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            if (requests == 2) {
                respond("""{"isSuccess":false}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("""{"IsSuccess":true,"Data":{"shopDto":{"name":"A"},"menu":{"photos":[{"id":"a","fullUrl":"https://photo/a"}]}}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repo = createShopMenuGalleryRepository(client)
            assertEquals(listOf("a"), repo.getMenuGallery("shop-1").getOrThrow().photos.map { it.id })
            assertTrue(repo.getMenuGallery("shop-1").isFailure)
            assertTrue(repo.getMenuGallery("bad/id").isFailure)
            assertTrue(repo.getMenuGallery("bad?query").isFailure)
            assertEquals(2, requests)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun httpFailureIsResultAndCancellationPropagates(): Unit = runBlocking {
        val errorEngine = MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) }
        val errorClient = HttpClientFactory(errorEngine).api("https://example.com")
        try {
            assertTrue(createShopMenuGalleryRepository(errorClient).getMenuGallery("shop-1").isFailure)
        } finally {
            errorClient.close()
            errorEngine.close()
        }
        val cancelledEngine = MockEngine { throw CancellationException("cancelled") }
        val cancelledClient = HttpClientFactory(cancelledEngine).api("https://example.com")
        try {
            assertFailsWith<CancellationException> {
                createShopMenuGalleryRepository(cancelledClient).getMenuGallery("shop-1")
            }
        } finally {
            cancelledClient.close()
            cancelledEngine.close()
        }
    }
}
