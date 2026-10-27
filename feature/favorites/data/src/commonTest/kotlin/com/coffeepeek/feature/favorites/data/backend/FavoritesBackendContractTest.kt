package com.coffeepeek.feature.favorites.data.backend

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class FavoritesBackendContractTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun readsKindAndPublicAddressFromTheCurrentAccountsList() = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/favorites", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            respond("""{"isSuccess":true,"data":[{"kind":"roaster","address":{"slug":"sample","canonicalPath":"/roasters/sample","revision":2,"isAlias":false},"createdAtUtc":"2026-10-15T10:00:00Z"}]}""", headers = jsonHeaders)
        }).api("https://api.example")
        try {
            val favorite = FavoritesBackend(client).getFavorites().getOrThrow().single()
            assertEquals("roaster", favorite.kind)
            assertEquals("sample", favorite.address?.slug)
            assertEquals(2, favorite.address?.revision)
            assertEquals("2026-10-15T10:00:00Z", favorite.createdAtUtc)
        } finally { client.close() }
    }

    @Test fun readsNamedDrinkWithPhotoAndNoPublicAddressAndKeepsUnderscoreSlug() = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            if (request.method == HttpMethod.Get) respond("""{"isSuccess":true,"data":[{"kind":"drink","createdAtUtc":"2026-10-15T10:00:00Z","name":"Матча латте","coverPhoto":{"fullUrl":"https://media.example/original.jpg","urls":{"thumbnail":"https://media.example/thumb.jpg","card":"https://media.example/card.jpg","detail":"https://media.example/detail.jpg","fullscreen":"https://media.example/full.jpg"}},"address":null,"drink":{"slug":"matcha_latte"}}]}""", headers = jsonHeaders)
            else {
                assertEquals("/api/v1/favorites/drink/matcha_latte", request.url.encodedPath)
                assertTrue(request.body is OutgoingContent.NoContent)
                respond("", HttpStatusCode.NoContent)
            }
        }).api("https://api.example")
        try {
            val service = FavoritesBackend(client)
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
        val client = HttpClientFactory(MockEngine { request ->
            methods += request.method
            assertEquals("/api/v1/favorites/coffee_shop/coffee%20%2F%20milk", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            assertTrue(request.body is OutgoingContent.NoContent)
            respond("", HttpStatusCode.NoContent)
        }).api("https://api.example")
        try {
            val service = FavoritesBackend(client)
            service.setFavorite("coffee_shop", "coffee / milk", true).getOrThrow()
            service.setFavorite("coffee_shop", "coffee / milk", false).getOrThrow()
            assertEquals(listOf(HttpMethod.Put, HttpMethod.Delete), methods)
        } finally { client.close() }
    }

    @Test fun rejectsFailedHttpAndListEnvelopes() = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            if (request.method == HttpMethod.Get) respond("""{"isSuccess":false,"message":"Unavailable","data":null}""", headers = jsonHeaders)
            else respond("", HttpStatusCode.ServiceUnavailable)
        }).api("https://api.example")
        try {
            val service = FavoritesBackend(client)
            assertTrue(service.getFavorites().isFailure)
            assertTrue(service.setFavorite("roaster", "sample", true).isFailure)
        } finally { client.close() }
    }
}
