package com.coffeepeek.admin.feature.coffee

import com.coffeepeek.admin.feature.coffee.data.CoffeeRepositoryImpl
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeeAvailability
import com.coffeepeek.admin.feature.coffee.domain.CoffeeOffer
import com.coffeepeek.admin.feature.coffee.ui.formatCoffeePrice
import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoffeeCatalogTest {
    @Test
    fun detailsMapCanonicalAddressCharacteristicsAndSafeOffers(): Unit = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/missing")) respond("not found", HttpStatusCode.NotFound)
            else {
                assertEquals("/api/v1/coffees/old-slug", request.url.encodedPath)
                respond(
                    """{"isSuccess":true,"data":{
                        "address":{"slug":"coffee","canonicalPath":"/coffees/coffee"},
                        "name":"Кофе","description":"Описание",
                        "roaster":{"name":"Roast","address":{"slug":"roast"}},
                        "photos":[{"fullUrl":"full.png","urls":{"card":"card.png","detail":"detail.png"}}],
                        "productKind":"roasted_beans","productForm":"whole_beans",
                        "classification":{"roastLevel":"light","acidity":"low","tasteGroups":["tropical"]},
                        "tasteDescriptors":["Херес","Черешня"],"catalogCheckedAtUtc":"2026-10-07T07:58:08Z",
                        "offers":[
                            {"offerKey":"a","weightGrams":200,"price":18.7,"currency":"BYN","availability":"InStock",
                             "sellerName":"Roast.by","sourceUrl":"https://roast.by/coffee","availabilityScope":"online",
                             "checkedAtUtc":"2026-10-07T07:58:08Z"},
                            {"weightGrams":1000,"price":70,"currency":"BYN","availability":"OutOfStock"},
                            {"weightGrams":0,"price":-1,"availability":"unexpected","sourceUrl":"javascript:alert(1)"}
                        ]
                    }}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = CoffeeRepositoryImpl(client)
            val details = repository.getDetails("old-slug").getOrThrow()
            assertEquals("/coffees/coffee", details.canonicalPath)
            assertEquals("Описание", details.description)
            assertEquals(listOf("detail.png"), details.photos)
            assertEquals("card.png", details.coffee.photoUrl)
            assertEquals("roast", details.roasterSlug)
            assertEquals("whole_beans", details.productForm)
            assertEquals("light", details.roastLevel)
            assertEquals("low", details.acidity)
            assertEquals(listOf("Херес", "Черешня"), details.tasteDescriptors)
            val offers = details.coffee.offers
            assertEquals(3, offers.size)
            assertEquals("https://roast.by/coffee", offers[0].sourceUrl)
            assertEquals("Roast.by", offers[0].sellerName)
            assertEquals(CoffeeAvailability.InStock, offers[0].availability)
            assertEquals(CoffeeAvailability.OutOfStock, offers[1].availability)
            assertEquals(CoffeeAvailability.Unknown, offers[2].availability)
            assertNull(offers[2].sourceUrl)
            assertNull(offers[2].price)
            assertNull(offers[2].weightGrams)
            assertTrue(repository.getDetails(" ").isFailure)
            assertTrue(repository.getDetails("missing").isFailure)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun searchContractMapsPhotosOffersAndPagination(): Unit = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/coffees/search", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals("Roast", body["q"]?.jsonPrimitive?.content)
            assertEquals("2", body["page"]?.jsonPrimitive?.content)
            assertEquals("20", body["pageSize"]?.jsonPrimitive?.content)
            assertEquals("tropical", body["filters"]?.jsonObject?.get("taste")?.jsonArray?.single()?.jsonPrimitive?.content)
            assertEquals("true", body["filters"]?.jsonObject?.get("availableOnly")?.jsonPrimitive?.content)
            respond(
                """{
                    "isSuccess":true,"message":"Operation successful",
                    "data":{"currentPage":2,"totalPages":3,"totalItems":48,"pageSize":20,"items":[
                        {"address":{"slug":"ne-vinnyy-kofe","revision":1},"name":"(Не)винный кофе",
                         "roaster":{"name":"Roast","coverPhoto":{"fullUrl":"logo.svg","urls":{"thumbnail":"logo-thumb.png"}}},
                         "coverPhoto":{"fullUrl":"photo.png","urls":{"card":"photo-card.png"}},
                         "countries":[{"code":"CO","nameRu":"Колумбия","nameEn":"Colombia"}],
                         "classification":{"tasteGroups":["tropical"],"roastLevel":"light"},
                         "matchingOffers":[{"price":18.7,"currency":"BYN","weightGrams":200},{"price":70,"currency":"BYN","weightGrams":1000}]},
                        {"address":{"slug":"unknown-weight"},"name":"Кофе","roaster":{"name":"Daloni"},
                         "coverPhoto":null,"classification":null,"countries":[{"code":"BR","nameRu":null,"nameEn":"Brazil"}],
                         "matchingOffers":[{"price":45,"currency":"BYN","weightGrams":null}]},
                        {"address":{"slug":"no-offers"},"name":"Без цены","roaster":{"name":"Roast"}}
                    ]}
                }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val page = CoffeeRepositoryImpl(client).search(
                " Roast ", CoffeeFilters(mapOf("taste" to setOf("tropical")), availableOnly = true), 2,
            ).getOrThrow()
            assertEquals(2, page.currentPage)
            assertEquals(3, page.totalPages)
            val coffee = page.items.first()
            assertEquals("photo-card.png", coffee.photoUrl)
            assertEquals("logo-thumb.png", coffee.roasterPhotoUrl)
            assertEquals(listOf("Колумбия"), coffee.countries)
            assertEquals(listOf("tropical"), coffee.tasteCodes)
            assertEquals(listOf(CoffeeOffer(18.7, "BYN", 200), CoffeeOffer(70.0, "BYN", 1000)), coffee.offers)
            assertNull(page.items[1].offers.single().weightGrams)
            assertEquals(listOf("Brazil"), page.items[1].countries)
            assertTrue(page.items[2].offers.isEmpty())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun catalogLabelsAndRequestFailures(): Unit = runBlocking {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/catalogs/coffee-filter-values" -> respond(
                    """{"isSuccess":true,"data":[{"code":"taste","name":"Вкусовой профиль","values":[
                        {"code":"nut","name":"Орехи","sortOrder":20},
                        {"code":"tropical","name":"Тропические фрукты","sortOrder":10}]}]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond("unavailable", HttpStatusCode.ServiceUnavailable)
            }
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = CoffeeRepositoryImpl(client)
            assertEquals("Тропические фрукты", repository.getFilterGroups().getOrThrow().single().options.first().name)
            assertTrue(repository.search("", CoffeeFilters(), 1).isFailure)
            assertTrue(repository.search("x".repeat(101), CoffeeFilters(), 1).isFailure)
        } finally {
            client.close()
            engine.close()
        }
        val cancelledEngine = MockEngine { throw CancellationException("cancelled search") }
        val cancelledClient = HttpClientFactory(cancelledEngine).api("https://example.com")
        try {
            var cancelled = false
            try { CoffeeRepositoryImpl(cancelledClient).search("", CoffeeFilters(), 1) }
            catch (_: CancellationException) { cancelled = true }
            assertTrue(cancelled)
        } finally {
            cancelledClient.close()
            cancelledEngine.close()
        }
    }

    @Test
    fun filterDraftAndPricePrecision() {
        val original = CoffeeFilters(availableOnly = true)
        val selected = original.toggle("taste", "tropical")
        assertEquals(2, selected.activeCount)
        assertEquals(original, selected.toggle("taste", "tropical"))
        assertFalse(original.values.containsKey("taste"))
        assertEquals("45", formatCoffeePrice(45.0))
        assertEquals("18.7", formatCoffeePrice(18.7))
        assertEquals("22.99", formatCoffeePrice(22.99))
        assertEquals("0.05", formatCoffeePrice(0.05))
    }
}
