package com.coffeepeek.api.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoasterContractTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun readsPublicArrayWithPhotosTagsAndCountsWithoutSearchOrPagination() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/roasters", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            respond("""{"isSuccess":true,"statusCode":200,"data":[
                {"address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":1},
                 "name":"Roast","photoUrl":"https://media.example/roast.svg",
                 "coverPhoto":{"id":"photo","fullUrl":"https://media.example/roast.svg","urls":{"card":"https://media.example/card/roast.svg"}},
                 "tags":[{"slug":"decaf","name":"Декаф","description":null,"sortOrder":2}],
                 "coffeeShopsCount":7,"coffeeProductsCount":21,"availableCoffeeProducts":21},
                {"address":null,"name":null,"photoUrl":null,"coverPhoto":null,"tags":null,
                 "coffeeShopsCount":"0","coffeeProductsCount":"0","availableCoffeeProducts":"0"}
            ]}""", headers = headers)
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val catalog = RoasterApiService(client).getRoasters().getOrThrow()
            assertEquals(2, catalog.size)
            assertEquals("roast", catalog.first().address?.slug)
            assertEquals("https://media.example/card/roast.svg", catalog.first().coverPhoto?.urls?.card)
            assertEquals("decaf", catalog.first().tags?.single()?.slug)
            assertEquals(7, catalog.first().coffeeShopsCount)
            assertEquals(21, catalog.first().coffeeProductsCount)
            assertEquals(21, catalog.first().availableCoffeeProducts)
            assertNull(catalog.last().address)
            assertEquals(0, catalog.last().coffeeShopsCount)
        } finally { client.close() }
    }

    @Test
    fun rejectsHttpAndEnvelopeErrorsInsteadOfCachingThemAsEmptyCatalogs() = runBlocking {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.ServiceUnavailable)) {
            val client = HttpClient(MockEngine {
                respond("""{"isSuccess":false,"message":"Unavailable","data":null}""", status, headers)
            }) { install(ContentNegotiation) { json() } }
            try {
                assertTrue(RoasterApiService(client).getRoasters().isFailure)
            } finally { client.close() }
        }
    }
}
