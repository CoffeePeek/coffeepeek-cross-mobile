package com.coffeepeek.api.service

import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicAddressContractTest {
    private val address = """{"slug":"26-october-16","canonicalPath":"/coffee-shops/26-october-16","revision":2,"isAlias":true}"""
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun detailsAndFiltersUseSlugsAndDirectData() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath == "/api/CoffeeShops/old-address") {
                respond("""{"isSuccess":true,"message":null,"data":{"address":$address,"name":"26","beans":[{"slug":"arabica","name":"Arabica"}],"canCreateReview":false,"existingReviewId":"review-guid"}}""", headers = headers)
            } else {
                assertEquals("minsk", request.url.parameters["city"])
                assertNull(request.url.parameters["cityId"])
                assertEquals(listOf("dog-friendly", "laptop-friendly"), request.url.parameters.getAll("tags"))
                assertNull(request.url.parameters["beans"])
                respond("""{"isSuccess":true,"message":null,"data":{"coffeeShops":[{"address":$address,"name":"26"}],"totalItems":1}}""", headers = headers)
            }
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val api = ShopApiService(client)
            val details = api.getShopDetails("old-address").getOrThrow()
            assertEquals("26-october-16", details.address.slug)
            assertTrue(details.address.isAlias)
            assertEquals("arabica", details.coffeeBeans.single().key)
            assertFalse(details.canCreateReview == true)
            assertEquals("review-guid", details.existingReviewId)
            assertEquals(1, api.searchShops(cityId = "minsk", tagIds = listOf("dog-friendly", "laptop-friendly"), beanIds = emptyList()).getOrThrow().totalItems)
        } finally { client.close() }
    }

    @Test
    fun allCatalogsReadArraysDirectly() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertTrue(request.url.parameters.isEmpty())
            val item = when (request.url.encodedPath.substringAfterLast('/')) {
                "cities" -> """{"address":$address,"name":"Catalog"}"""
                "roasters" -> """{
                    "address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":"2","isAlias":false},
                    "name":null,"photoUrl":null,
                    "coverPhoto":{"id":"photo","fullUrl":null,"sortIndex":"0","isPrimary":true,"urls":{"card":"https://m/card.jpg"}},
                    "tags":[{"slug":"specialty","name":"Specialty","description":null,"sortOrder":"2"}],
                    "coffeeShopsCount":"7","coffeeProductsCount":12,"availableCoffeeProducts":"4"
                }"""
                else -> """{"slug":"catalog-slug","name":"Catalog"}"""
            }
            respond("""{"isSuccess":true,"message":null,"data":[$item]}""", headers = headers)
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val api = ShopApiService(client)
            assertEquals("26-october-16", api.getCities().getOrThrow().single().address.slug)
            val roaster = RoasterApiService(client).getRoasters().getOrThrow().single()
            assertEquals("roast", roaster.address?.slug)
            assertEquals(2, roaster.address?.revision)
            assertNull(roaster.name)
            assertEquals("https://m/card.jpg", roaster.coverPhoto?.urls?.card)
            assertEquals(7, roaster.coffeeShopsCount)
            assertEquals(12, roaster.coffeeProductsCount)
            assertEquals(4, roaster.availableCoffeeProducts)
            assertEquals("specialty", roaster.tags?.single()?.slug)
            assertEquals(2, roaster.tags?.single()?.sortOrder)
            assertEquals("catalog-slug", api.getBeans().getOrThrow().single().key)
            assertEquals("catalog-slug", api.getEquipment().getOrThrow().single().key)
            assertEquals("catalog-slug", api.getBrewMethods().getOrThrow().single().key)
            assertEquals("catalog-slug", api.getShopTags().getOrThrow().single().key)
        } finally { client.close() }
    }

    @Test
    fun reviewsCheckInsAndSubmissionsReadItemsAndNullableLinks() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            val items = when (request.url.encodedPath) {
                "/api/CheckIns" -> {
                    assertEquals("2", request.headers["X-Page-Number"])
                    assertEquals("20", request.headers["X-Page-Size"])
                    """[{"id":"check-in-guid","shop":null}]"""
                }
                "/api/users/petr/reviews" -> {
                    assertEquals("2", request.url.parameters["pageNumber"])
                    """[{"id":"review-guid","author":null,"shop":$address}]"""
                }
                "/api/ModerationShops/mine" -> """[{"id":"submission-guid","moderationStatus":"Approved","publishedShop":$address}]"""
                "/api/ModerationReviews/mine" -> """[{"id":"submission-guid","moderationStatus":"Pending","shop":null}]"""
                "/api/ModerationRoasters/mine" -> """[{"id":"submission-guid","moderationStatus":"Pending"}]"""
                "/api/ShopChangeRequests/mine" -> {
                    assertEquals("26-october-16", request.url.parameters["shop"])
                    assertNull(request.url.parameters["shopId"])
                    """[{"id":"submission-guid","shop":null,"section":"Tags","payload":{"tags":[]},"status":"Pending","createdAtUtc":"2026-09-30T12:00:00Z"}]"""
                }
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
            respond("""{"isSuccess":true,"message":null,"data":{"items":$items,"totalItems":1,"currentPage":2,"pageSize":20}}""", headers = headers)
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            assertNull(CheckInApiService(client).getMyCheckIns(2, 20).getOrThrow().checkIns.single().shop)
            assertNull(ReviewApiService(client).getUserReviews("petr", 2, 20).getOrThrow().reviewDtos.single().author)
            assertEquals("26-october-16", ShopApiService(client).getMyModerationShops(ModerationStatusDto.Approved, 2, 20).getOrThrow().moderationShops.single().publishedShop?.slug)
            assertEquals(1, ReviewApiService(client).getMyModerationReviews(ModerationStatusDto.Pending, 2, 20).getOrThrow().reviewDtos.size)
            assertEquals(1, RoasterApiService(client).getMyModerationRoasters(ModerationStatusDto.Pending, 2, 20).getOrThrow().items.size)
            val changes = ShopChangeRequestApiService(client).getMine(2, 20, shopId = "26-october-16").getOrThrow()
            assertEquals(emptyList(), changes.items.single().payload.tagIds)
        } finally { client.close() }
    }

    @Test
    fun publicProfileAndUsernameResponseKeepServerAddress() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/Users/by-slug/petr" -> respond("""{"data":{"userName":"Petr"},"address":$address}""", headers = headers)
                "/api/Users/me/username" -> respond("""{"isSuccess":true,"message":null,"data":{"username":"Petr","address":null}}""", HttpStatusCode.Accepted, headers)
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val api = UserApiService(client)
            assertEquals("Petr", api.getUser("petr").getOrThrow().userName)
            assertNull(api.updateUsername("Petr").getOrThrow().address)
        } finally { client.close() }
    }

    @Test
    fun unavailableAddressAndUnknownFilterRemainFailures() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            val status = if (request.url.parameters["city"] != null) HttpStatusCode.NotFound else HttpStatusCode.ServiceUnavailable
            respond("""{"isSuccess":false,"message":null,"data":null}""", status, headers)
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val api = ShopApiService(client)
            assertTrue(api.getShopDetails("26-october-16").isFailure)
            assertTrue(api.searchShops(cityId = "unknown").isFailure)
        } finally { client.close() }
    }
}
