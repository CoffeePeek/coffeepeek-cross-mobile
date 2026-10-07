package com.coffeepeek.data.feature.feed

import com.coffeepeek.api.feature.feed.FeedApiService
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedLoadException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class FeedRepositoryContractTest {
    @Test
    fun mapsPublicationOrderSeparateVisitsAndProtectedUrlsWithoutStorageKeyFallback() = runBlocking {
        val client = client(MockEngine {
            respond("""{"isSuccess":true,"data":{"items":[
                {"publishedAtUtc":"2026-10-07T10:00:00Z","checkIn":{"id":"old-visit","createdAtUtc":"2020-01-01T00:00:00Z","visitedAt":"2019-01-01T00:00:00Z","shop":null,"author":null,"text":"First","rating":{"coffee":5,"service":4,"place":4},"photos":[{"sortIndex":1,"url":"https://api.example/protected-second"},{"sortIndex":0,"url":"/api/v1/check-ins/old-visit/photos/first"},{"sortIndex":2,"url":"","storageKey":"never-public.jpg"}]}},
                {"publishedAtUtc":"2026-10-07T09:00:00Z","checkIn":{"id":"new-visit","createdAtUtc":"2026-10-07T08:00:00Z","text":"Second","rating":{"coffee":4,"service":4,"place":4}}}
            ],"nextCursor":"feed-cursor"}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val page = FeedRepositoryImpl(FeedApiService(client), FileUrlResolver("https://api.example/")).getFeed(20, null, FeedFilters()).getOrThrow()
            assertEquals(listOf("old-visit", "new-visit"), page.items.map { it.checkIn.id })
            assertEquals("2026-10-07T10:00:00Z", page.items.first().publishedAtUtc)
            assertEquals("2019-01-01T00:00:00Z", page.items.first().checkIn.visitedAt)
            assertEquals(listOf("https://api.example/api/v1/check-ins/old-visit/photos/first", "https://api.example/protected-second"), page.items.first().checkIn.photoUrls)
            assertNull(page.items.first().checkIn.authorAddress)
            assertNull(page.items.first().checkIn.shopAddress)
            assertEquals("feed-cursor", page.nextCursor)
        } finally { client.close() }
    }

    @Test
    fun invalidContinuationRequiresRestartButUnknownFilterDoesNotBecomeGlobalFeed() = runBlocking {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.NotFound)) {
            var requests = 0
            val client = client(MockEngine { requests++; respond("", status) })
            try {
                val error = FeedRepositoryImpl(FeedApiService(client), FileUrlResolver("https://api.example/"))
                    .getFeed(20, "old-cursor", FeedFilters(citySlug = "unknown")).exceptionOrNull() as FeedLoadException
                assertEquals(status == HttpStatusCode.BadRequest, error.restartPagination)
                assertEquals(1, requests)
            } finally { client.close() }
        }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("https://api.example") }
    }
}
