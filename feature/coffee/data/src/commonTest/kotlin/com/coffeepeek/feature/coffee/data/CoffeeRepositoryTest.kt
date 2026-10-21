package com.coffeepeek.feature.coffee.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.coffee.data.repository.createCoffeeRepository
import com.coffeepeek.feature.coffee.domain.model.CoffeeAvailability
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoffeeRepositoryTest {
    @Test
    fun searchPreservesWireBodyMethodAndPagination() = runTest {
        withRepository(
            """{"isSuccess":true,"data":{"items":[],"currentPage":2,"totalPages":4,"totalCount":67}}""",
            inspect = { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/api/v1/coffees/search", request.url.encodedPath)
                val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                assertEquals("Roast", body.getValue("q").jsonPrimitive.content)
                assertEquals("2", body.getValue("page").jsonPrimitive.content)
                assertEquals("20", body.getValue("pageSize").jsonPrimitive.content)
                val filters = body.getValue("filters").jsonObject
                assertEquals(listOf("nut", "tropical"), filters.getValue("taste").jsonArray.map { it.jsonPrimitive.content })
                assertEquals("true", filters.getValue("availableOnly").jsonPrimitive.content)
                assertFalse("roast" in filters)
            },
        ) { repository ->
            val page = repository.search(" Roast ", CoffeeFilters(mapOf("taste" to setOf("tropical", "nut"), "roast" to emptySet()), true), 2).getOrThrow()
            assertEquals(2, page.currentPage)
            assertEquals(4, page.totalPages)
            assertEquals(67, page.totalCount)
            assertTrue(page.items.isEmpty())
        }
    }

    @Test
    fun searchMapsCountriesPhotosAndMatchingOffers() = runTest {
        withRepository("""{"isSuccess":true,"data":{"currentPage":1,"totalPages":1,"items":[{
            "address":{"slug":"coffee"},"name":"Coffee",
            "roaster":{"name":"Roast","coverPhoto":{"fullUrl":"logo","urls":{"thumbnail":"thumb"}}},
            "coverPhoto":{"fullUrl":"full","urls":{"card":"card"}},
            "countries":[{"code":"CO","nameRu":"Колумбия","nameEn":"Colombia"},{"code":"BR","nameEn":"Brazil"},{"code":"ET"}],
            "classification":{"tasteGroups":["tropical"]},
            "matchingOffers":[{"price":18.7,"weightGrams":200,"currency":"BYN"}],
            "offers":[{"price":70,"weightGrams":1000}]
        }]}}""") { repository ->
            val page = repository.search("", CoffeeFilters(), 1).getOrThrow()
            assertNull(page.totalCount)
            val coffee = page.items.single()
            assertEquals("coffee", coffee.slug)
            assertEquals("card", coffee.photoUrl)
            assertEquals("thumb", coffee.roasterPhotoUrl)
            assertEquals(listOf("Колумбия", "Brazil", "ET"), coffee.countries)
            assertEquals(listOf("tropical"), coffee.tasteCodes)
            assertEquals(18.7, coffee.offers.single().price)
            assertEquals(200, coffee.offers.single().weightGrams)
        }
    }

    @Test
    fun detailsPreserveCanonicalAddressCharacteristicsAndGallery() = runTest {
        withRepository("""{"isSuccess":true,"data":{
            "address":{"slug":"canonical","canonicalPath":"/coffees/canonical"},"name":"Coffee",
            "roaster":{"name":"Roast","address":{"slug":"roast"}},
            "photos":[{"fullUrl":"full","urls":{"card":"card","detail":"detail"}}],
            "description":"Description","productKind":"roasted_beans","productForm":"whole_beans",
            "classification":{"roastLevel":"light","acidity":"low","tasteGroups":["tropical"]},
            "tasteDescriptors":["Cherry"],"catalogCheckedAtUtc":"2026-10-07T07:58:08Z"
        }}""", inspect = {
            assertEquals(HttpMethod.Get, it.method)
            assertEquals("/api/v1/coffees/old-slug", it.url.encodedPath)
        }) { repository ->
            val details = repository.getDetails("old-slug").getOrThrow()
            assertEquals("/coffees/canonical", details.canonicalPath)
            assertEquals("Description", details.description)
            assertEquals("roast", details.roasterSlug)
            assertEquals(listOf("detail"), details.photos)
            assertEquals("card", details.coffee.photoUrl)
            assertEquals("roasted_beans", details.productKind)
            assertEquals("whole_beans", details.productForm)
            assertEquals("light", details.roastLevel)
            assertEquals("low", details.acidity)
            assertEquals(listOf("Cherry"), details.tasteDescriptors)
            assertEquals("2026-10-07T07:58:08Z", details.checkedAtUtc)
        }
    }

    @Test
    fun absentOptionalDetailsUseExistingFallbacks() = runTest {
        withRepository("""{"isSuccess":true,"data":{
            "address":{"slug":"coffee","canonicalPath":"https://elsewhere.invalid"},"name":"Coffee",
            "roaster":{"name":"Roast"},"description":" ","coverPhoto":{"fullUrl":"cover"},
            "offers":[{"price":45,"currency":"BYN"}]
        }}""") { repository ->
            val details = repository.getDetails("coffee").getOrThrow()
            assertEquals("/coffees/coffee", details.canonicalPath)
            assertNull(details.description)
            assertNull(details.roasterSlug)
            assertEquals(listOf("cover"), details.photos)
            assertEquals("cover", details.coffee.photoUrl)
            assertEquals(45.0, details.coffee.offers.single().price)
            assertTrue(details.coffee.countries.isEmpty())
        }
    }

    @Test
    fun offersPreserveUnknownAvailabilityAndRejectUnsafeLinksOrInvalidValues() = runTest {
        withRepository("""{"isSuccess":true,"data":{
            "address":{"slug":"coffee"},"name":"Coffee","roaster":{"name":"Roast"},"offers":[
            {"offerKey":"a","price":0,"weightGrams":200,"availability":"InStock","sourceUrl":"https://seller.invalid/item",
             "sellerName":"Seller","grind":"whole_beans","brewPurpose":"espresso","availabilityScope":"online","checkedAtUtc":"now"},
            {"availability":"OutOfStock","sourceUrl":"http://seller.invalid/item"},
            {"availability":"PreOrder"},
            {"price":-1,"weightGrams":0,"availability":"new_value","sourceUrl":"javascript:alert(1)"},
            {"sourceUrl":"file:///private/photo"}
        ]}}""") { repository ->
            val offers = repository.getDetails("coffee").getOrThrow().coffee.offers
            assertEquals(listOf(CoffeeAvailability.InStock, CoffeeAvailability.OutOfStock, CoffeeAvailability.PreOrder,
                CoffeeAvailability.Unknown, CoffeeAvailability.Unknown), offers.map { it.availability })
            assertEquals(0.0, offers[0].price)
            assertEquals("a", offers[0].key)
            assertEquals("Seller", offers[0].sellerName)
            assertEquals("whole_beans", offers[0].grind)
            assertEquals("espresso", offers[0].brewPurpose)
            assertEquals("online", offers[0].availabilityScope)
            assertEquals("now", offers[0].checkedAtUtc)
            assertEquals("https://seller.invalid/item", offers[0].sourceUrl)
            assertEquals("http://seller.invalid/item", offers[1].sourceUrl)
            assertNull(offers[3].price)
            assertNull(offers[3].weightGrams)
            assertNull(offers[3].sourceUrl)
            assertNull(offers[4].sourceUrl)
        }
    }

    @Test
    fun filtersHideUnknownGroupsAndSortOptionsStably() = runTest {
        withRepository("""{"isSuccess":true,"data":[
            {"code":"taste","name":"Taste","values":[{"code":"nut","name":"Nut","sortOrder":20},{"code":"fruit","name":"Fruit","sortOrder":10}]},
            {"code":"future","name":"Future","values":[]}
        ]}""", inspect = {
            assertEquals(HttpMethod.Get, it.method)
            assertEquals("/api/v1/catalogs/coffee-filter-values", it.url.encodedPath)
        }) { repository ->
            val group = repository.getFilterGroups().getOrThrow().single()
            assertEquals("Taste", group.name)
            assertEquals(listOf("fruit", "nut"), group.options.map { it.code })
            assertEquals(listOf("Fruit", "Nut"), group.options.map { it.name })
        }
    }

    @Test
    fun searchAcceptsExactlyTheEightExistingFilterCodes() = runTest {
        val codes = listOf("brew", "caffeine", "roast", "acidity", "processing", "fermentation", "taste", "composition")
        withRepository(emptyPage, inspect = {
            val body = Json.parseToJsonElement(it.body.toByteArray().decodeToString()).jsonObject
            val filters = body.getValue("filters").jsonObject
            assertEquals((codes + "availableOnly").toSet(), filters.keys)
            assertEquals("false", filters.getValue("availableOnly").jsonPrimitive.content)
        }) { repository ->
            assertTrue(repository.search("", CoffeeFilters(codes.associateWith { setOf("value") }), 1).isSuccess)
        }
    }

    @Test
    fun invalidInputsFailBeforeAnyHttpRequest() = runTest {
        var calls = 0
        withRepository(emptyPage, inspect = { calls++ }) { repository ->
            assertTrue(repository.search("x".repeat(101), CoffeeFilters(), 1).isFailure)
            assertTrue(repository.search("", CoffeeFilters(), 0).isFailure)
            assertTrue(repository.search("", CoffeeFilters(), 100_001).isFailure)
            assertTrue(repository.search("", CoffeeFilters(mapOf("unknown" to emptySet())), 1).isFailure)
            assertTrue(repository.getDetails(" ").isFailure)
            assertTrue(repository.getDetails("x".repeat(201)).isFailure)
            assertEquals(0, calls)
        }
    }

    @Test
    fun maximumQueryAndPageRemainValid() = runTest {
        withRepository(emptyPage) { repository ->
            assertTrue(repository.search("x".repeat(100), CoffeeFilters(), 100_000).isSuccess)
        }
    }

    @Test
    fun slugRemainsOneEncodedPathSegment() = runTest {
        withRepository(minimalDetails, inspect = {
            assertEquals("/api/v1/coffees/coffee%2Fother%3Fx%23y", it.url.encodedPath)
            assertTrue(it.url.parameters.isEmpty())
        }) { repository ->
            assertTrue(repository.getDetails("coffee/other?x#y").isSuccess)
        }
    }

    @Test
    fun httpErrorsReturnFailuresForEveryEndpoint() = runTest {
        withRepository("unavailable", status = HttpStatusCode.ServiceUnavailable) { repository ->
            assertAllFailures(repository)
        }
    }

    @Test
    fun unsuccessfulOrMissingDataEnvelopeIsNotSuccess() = runTest {
        for (response in listOf("""{"isSuccess":false,"message":"Failure"}""", """{"isSuccess":true}""")) {
            withRepository(response) { repository -> assertAllFailures(repository) }
        }
    }

    @Test
    fun malformedResponseIsNotSuccess() = runTest {
        withRepository("{not json}") { repository -> assertAllFailures(repository) }
    }

    @Test
    fun cancellationEscapesResultForEveryEndpoint() = runTest {
        val engine = MockEngine { throw CancellationException("test cancellation") }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try {
            val repository = createCoffeeRepository(client)
            assertFailsWith<CancellationException> { repository.search("", CoffeeFilters(), 1) }
            assertFailsWith<CancellationException> { repository.getDetails("coffee") }
            assertFailsWith<CancellationException> { repository.getFilterGroups() }
        } finally {
            client.close()
            engine.close()
        }
    }

    private suspend fun assertAllFailures(repository: CoffeeRepository) {
        assertTrue(repository.search("", CoffeeFilters(), 1).isFailure)
        assertTrue(repository.getDetails("coffee").isFailure)
        assertTrue(repository.getFilterGroups().isFailure)
    }

    private suspend fun withRepository(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        inspect: suspend (HttpRequestData) -> Unit = {},
        block: suspend (CoffeeRepository) -> Unit,
    ) {
        val engine = MockEngine { request ->
            inspect(request)
            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try {
            block(createCoffeeRepository(client))
        } finally {
            client.close()
            engine.close()
        }
    }
}

private const val emptyPage = """{"isSuccess":true,"data":{"items":[],"currentPage":1,"totalPages":0}}"""
private const val minimalDetails = """{"isSuccess":true,"data":{"address":{"slug":"coffee"},"name":"Coffee","roaster":{"name":"Roast"}}}"""
