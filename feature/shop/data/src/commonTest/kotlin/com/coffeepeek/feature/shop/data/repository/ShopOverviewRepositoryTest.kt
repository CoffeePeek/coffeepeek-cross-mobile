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

class ShopOverviewRepositoryTest {
    @Test fun mapsHeaderFieldsAndPhotoVariantsWithoutLoadingLegacyModels() = runBlocking {
        val engine = MockEngine {
            respond("""{
              "isSuccess":true,
              "data":{"shopDto":{
                "id":"shop-1","name":"Coffee","description":"  Fresh coffee  ",
                "location":{"address":" Main street ","latitude":53.9,"longitude":27.5},
                "rating":4.6,"reviewCount":12,"isOpen":true,
                "photos":[
                  {"id":"first","fullUrl":"https://photo/original","urls":{"detail":"https://photo/hero","fullscreen":"https://photo/full"}},
                  {"id":"second","fullUrl":"https://photo/second"},
                  {"id":"missing"}
                ],
                "menu":{"photos":[]},"otherField":"ignored"
              }}
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val overview = createShopOverviewRepository(client).getOverview("shop-1").getOrThrow()
            assertEquals("shop-1", overview.id)
            assertEquals("Coffee", overview.title)
            assertEquals("Fresh coffee", overview.description)
            assertEquals("Main street", overview.address)
            assertEquals(53.9, overview.latitude)
            assertEquals(27.5, overview.longitude)
            assertEquals(4.6, overview.rating)
            assertEquals(12, overview.reviewCount)
            assertTrue(overview.isOpen)
            assertEquals(listOf("first", "second"), overview.photos.map { it.id })
            assertEquals("https://photo/hero", overview.photos.first().previewUrl)
            assertEquals("https://photo/full", overview.photos.first().fullUrl)
            assertEquals("https://photo/second", overview.photos.last().previewUrl)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun absentOptionalFieldsAndRejectedResponseRemainExplicit() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(if (calls == 1) {
                """{"isSuccess":true,"data":{"shopDto":{"name":"Unnamed rating"}}}"""
            } else {
                """{"isSuccess":false,"data":null}"""
            }, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopOverviewRepository(client)
            val overview = repository.getOverview("shop-1").getOrThrow()
            assertEquals("shop-1", overview.id)
            assertNull(overview.rating)
            assertNull(overview.address)
            assertTrue(overview.photos.isEmpty())
            assertTrue(repository.getOverview("shop-1").isFailure)
        } finally {
            client.close()
            engine.close()
        }
    }
}
