package com.coffeepeek.api.feature.favorites

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class FavoritesApiContractTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun readsKindAndPublicAddressFromTheCurrentAccountsList() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/favorites", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            respond("""{"isSuccess":true,"data":[{"kind":"roaster","address":{"slug":"sample","canonicalPath":"/roasters/sample","revision":2,"isAlias":false},"createdAtUtc":"2026-10-15T10:00:00Z"}]}""", headers = jsonHeaders)
        }) { install(ContentNegotiation) { json() } }
        try {
            val favorite = FavoritesApiService(client).getFavorites().getOrThrow().single()
            assertEquals("roaster", favorite.kind)
            assertEquals("sample", favorite.address?.slug)
            assertEquals(2, favorite.address?.revision)
            assertEquals("2026-10-15T10:00:00Z", favorite.createdAtUtc)
        } finally { client.close() }
    }

    @Test fun readsNamedDrinkWithPhotoAndNoPublicAddressAndKeepsUnderscoreSlug() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            if (request.method == HttpMethod.Get) respond("""{"isSuccess":true,"data":[{"kind":"drink","createdAtUtc":"2026-10-15T10:00:00Z","name":"Матча латте","coverPhoto":{"fullUrl":"https://media.example/original.jpg","urls":{"thumbnail":"https://media.example/thumb.jpg","card":"https://media.example/card.jpg","detail":"https://media.example/detail.jpg","fullscreen":"https://media.example/full.jpg"}},"address":null,"drink":{"slug":"matcha_latte"}}]}""", headers = jsonHeaders)
            else {
                assertEquals("/api/v1/favorites/drink/matcha_latte", request.url.encodedPath)
                assertTrue(request.body is OutgoingContent.NoContent)
                respond("", HttpStatusCode.NoContent)
            }
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = FavoritesApiService(client)
            val drink = service.getFavorites().getOrThrow().single()
            assertNull(drink.address)
            assertEquals("Матча латте", drink.name)
            assertEquals("matcha_latte", drink.drink?.slug)
            assertEquals("https://media.example/card.jpg", drink.coverPhoto?.urls?.card)
            service.setFavorite("drink", "matcha_latte", true).getOrThrow()
            service.setFavorite("drink", "matcha_latte", false).getOrThrow()
        } finally { client.close() }
    }

    @Test fun putsAndDeletesByKindAndEncodedSlugWithoutUserIdOrBodyAndAccepts204() = runBlocking {
        val methods = mutableListOf<HttpMethod>()
        val client = HttpClient(MockEngine { request ->
            methods += request.method
            assertEquals("/api/v1/favorites/coffee_shop/coffee%20%2F%20milk", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            assertTrue(request.body is OutgoingContent.NoContent)
            respond("", HttpStatusCode.NoContent)
        })
        try {
            val service = FavoritesApiService(client)
            service.setFavorite("coffee_shop", "coffee / milk", true).getOrThrow()
            service.setFavorite("coffee_shop", "coffee / milk", false).getOrThrow()
            assertEquals(listOf(HttpMethod.Put, HttpMethod.Delete), methods)
        } finally { client.close() }
    }

    @Test fun rejectsFailedHttpAndListEnvelopes() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            if (request.method == HttpMethod.Get) respond("""{"isSuccess":false,"message":"Unavailable","data":null}""", headers = jsonHeaders)
            else respond("", HttpStatusCode.ServiceUnavailable)
        }) { install(ContentNegotiation) { json() } }
        try {
            val service = FavoritesApiService(client)
            assertTrue(service.getFavorites().isFailure)
            assertTrue(service.setFavorite("roaster", "sample", true).isFailure)
        } finally { client.close() }
    }
}
