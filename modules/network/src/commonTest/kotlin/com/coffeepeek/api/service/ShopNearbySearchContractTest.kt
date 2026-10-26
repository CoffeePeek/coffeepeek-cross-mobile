package com.coffeepeek.api.service

import com.coffeepeek.api.model.request.ShopSearchFiltersDto
import com.coffeepeek.api.model.request.ShopSearchOriginDto
import com.coffeepeek.api.model.request.ShopSearchRequestDto
import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopNearbySearchContractTest {
    @Test
    fun ordinarySearchUsesPostWithoutQueryOrGeolocationAndPreservesPagination() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/coffee-shops/search", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertTrue("q" !in body && "sort" !in body)
            val filters = body.getValue("filters").jsonObject
            assertTrue("origin" !in filters && "radiusKm" !in filters)
            assertEquals("minsk", filters.getValue("city").jsonPrimitive.content)
            respond("""{"isSuccess":true,"data":{"items":[],"totalItems":21,"totalPages":2,"currentPage":2}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val page = ShopApiService(client).searchShops(query = "   ", cityId = "minsk", page = 2).getOrThrow()
            assertEquals(21, page.totalItems)
            assertEquals(2, page.currentPage)
        } finally { client.close() }
    }

    @Test
    fun invalidGeolocationAndPaginationNeverReachTransport() = runBlocking {
        val client = HttpClient(MockEngine { error("Invalid request reached the network") })
        try {
            val api = ShopApiService(client)
            assertTrue(api.searchShops(ShopSearchRequestDto(ShopSearchFiltersDto(radiusKm = 3), page = 1, pageSize = 20)).isFailure)
            assertTrue(api.searchShops(ShopSearchRequestDto(ShopSearchFiltersDto(), sort = "distance_asc", page = 1, pageSize = 20)).isFailure)
            assertTrue(api.searchShops(page = 0).isFailure)
            assertTrue(api.searchShops(pageSize = 101).isFailure)
        } finally { client.close() }
    }

    @Test
    fun sendsLocationAndFiltersAndReadsDiscoveryPage() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/coffee-shops/search", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            val filters = body.getValue("filters").jsonObject
            val origin = filters.getValue("origin").jsonObject
            assertEquals("53.902284", origin.getValue("latitude").jsonPrimitive.content)
            assertEquals("27.561831", origin.getValue("longitude").jsonPrimitive.content)
            assertEquals("3", filters.getValue("radiusKm").jsonPrimitive.content)
            if (body.getValue("page").jsonPrimitive.content == "1") {
                assertTrue("q" !in body)
            } else {
                assertEquals("minsk", filters.getValue("city").jsonPrimitive.content)
                assertEquals("true", filters.getValue("isOpen").jsonPrimitive.content)
                assertEquals("coffee", body.getValue("q").jsonPrimitive.content)
            }
            assertEquals("distance_asc", body.getValue("sort").jsonPrimitive.content)
            assertTrue(body.getValue("page").jsonPrimitive.content in setOf("1", "2"))
            assertEquals("20", body.getValue("pageSize").jsonPrimitive.content)
            respond("""{"isSuccess":true,"data":{"items":[{"address":{"slug":"shop","canonicalPath":"/coffee-shops/shop","revision":1,"isAlias":false},"name":"Shop","addressLine":"Main street","rating":4.5,"isVisited":null,"isFavorite":null,"distanceMeters":148,"tags":[{"slug":"wifi","name":"Wi-Fi"}]}],"totalItems":21,"totalPages":2,"currentPage":2}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val result = ShopApiService(client).searchShops(
                ShopSearchRequestDto(
                    filters = ShopSearchFiltersDto(
                        origin = ShopSearchOriginDto(53.902284, 27.561831),
                        radiusKm = 3,
                        city = "minsk",
                        isOpen = true,
                    ),
                    q = "coffee",
                    sort = "distance_asc",
                    page = 2,
                    pageSize = 20,
                ),
            ).getOrThrow()
            assertEquals(21, result.totalItems)
            assertEquals(2, result.totalPages)
            assertEquals(148.0, result.items.single().distanceMeters)
            assertEquals("Wi-Fi", result.items.single().tags.single().name)
            assertTrue(ShopApiService(client).searchShops(
                ShopSearchRequestDto(ShopSearchFiltersDto(ShopSearchOriginDto(53.902284, 27.561831), radiusKm = 3), sort = "distance_asc", page = 1, pageSize = 20),
            ).isSuccess)
            assertTrue(ShopApiService(client).searchShops(
                ShopSearchRequestDto(ShopSearchFiltersDto(ShopSearchOriginDto(91.0, 0.0)), page = 1, pageSize = 20),
            ).isFailure)
        } finally { client.close() }
    }
}
