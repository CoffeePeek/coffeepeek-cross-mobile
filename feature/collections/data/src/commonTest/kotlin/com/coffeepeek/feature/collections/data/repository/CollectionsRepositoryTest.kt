package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class CollectionsRepositoryTest {
    @Test fun publicListDispatchRequiresExplicitPublicVisibilityWhileLegacySnapshotsRemainReadable() = runTest {
        var response = body
        val client = client { respond(response) }
        try {
            val repository = createCollectionsRepository(client, client, "https://fixture.invalid", Account())
            assertEquals(SharedCollectionMode.Snapshot, repository.read(id).getOrThrow().mode)
            val rejected = listOf("\"mode\":\"list\"", "\"mode\":\"list\",\"visibility\":null",
                "\"mode\":\"list\",\"visibility\":\"private\"", "\"mode\":\"list\",\"visibility\":\"unknown\"",
                "\"mode\":\"unknown\",\"visibility\":\"public\"", "\"mode\":\"snapshot\",\"visibility\":\"private\"",
                "\"mode\":\"snapshot\",\"visibility\":\"unknown\"")
            rejected.forEach { fields ->
                response = body.replace("\"items\":", "$fields,\"items\":")
                assertTrue(repository.read(id).isFailure, fields)
            }
            response = body.replace("\"items\":", "\"mode\":\"list\",\"visibility\":\"public\",\"items\":")
            assertEquals(SharedCollectionMode.List, repository.read(id).getOrThrow().mode)
        } finally { client.close() }
    }

    @Test fun publicCardsKeepActualStatisticsAndGroupShopsBeforeRoasters() = runTest {
        val client = client { respond("""{"data":{"id":"$id","title":"Places","canonicalPath":"$path","items":[
            {"kind":"roaster","name":"Roaster","address":{"slug":"roaster"}},
            {"kind":"coffee_shop","name":"Shop","address":{"slug":"shop"},"rating":4.68,"checkInCount":124,"streetAddress":"Test street"},
            {"kind":"coffee_shop","name":"New shop","address":{"slug":"new-shop"},"rating":7,"checkInCount":-1}
        ]}}""") }
        try {
            val places = createCollectionsRepository(client, client, "https://fixture.invalid", Account()).read(id).getOrThrow().places
            assertEquals(listOf("shop", "new-shop", "roaster"), places.map { it.slug })
            assertEquals(4.68, places.first().rating)
            assertEquals(124, places.first().checkInCount)
            assertEquals("Test street", places.first().address)
            assertNull(places[1].rating)
            assertNull(places[1].checkInCount)
            assertNull(places.last().rating)
        } finally { client.close() }
    }

    private val id = "12345678-1234-1234-1234-1234567890ab"
    private val path = "/collections/${id.replace("-", "")}"
    private val roaster = CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Roaster")
    private val body get() = """{"data":{"id":"$id","title":"My places","canonicalPath":"$path","items":[{"kind":"roaster","name":"Roaster","address":{"slug":"roaster"}}]}}"""
    private class Account : CollectionsAccount {
        val owner = MutableStateFlow<String?>("first")
        var loads = 0
        override val identities = owner
        override fun currentId() = owner.value
        override suspend fun loadId(): String? { loads++; return currentId() }
    }

    @Test fun favoritesKeepBothKindsAndResolvePublicPhotosWithoutDrinks() = runTest {
        val client = client {
            respond("""{"data":[{"kind":"coffee_shop","name":"Shop","address":{"slug":"shop"},"coverPhoto":{"urls":{"card":"/photo/card.jpg"}}},{"kind":"roaster","name":"Roaster","address":{"slug":"roaster"}},{"kind":"drink","name":"Espresso"}]}""")
        }
        try {
            val values = createCollectionsRepository(client, client, "https://fixture.invalid", Account()).favorites().getOrThrow()
            assertEquals(listOf(CollectionPlaceKind.CoffeeShop, CollectionPlaceKind.Roaster), values.map { it.kind })
            assertEquals("https://fixture.invalid/photo/card.jpg", values.first().photoUrl)
        } finally { client.close() }
    }

    @Test fun createSendsOnlyExplicitSelectionAndTitleThenUsesVerifiedPublicAddress() = runTest {
        val client = client { request ->
            val payload = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
            assertEquals(setOf("title", "items"), payload.keys)
            assertEquals("My places", payload.getValue("title").jsonPrimitive.content)
            assertEquals(Json.parseToJsonElement("""[{"kind":"roaster","slug":"roaster"}]"""), payload["items"])
            respond(body)
        }
        try {
            val result = createCollectionsRepository(client, client, "https://fixture.invalid", Account()).create(" My places ", listOf(roaster)).getOrThrow()
            assertEquals("https://coffeepeek.by$path", result.url)
            assertEquals(listOf("roaster"), result.places.map { it.slug })
        } finally { client.close() }
    }

    @Test fun invalidSelectionsDoNotWriteAndForeignCanonicalPathsAreRejected() = runTest {
        var requests = 0
        val client = client { requests++; respond(body.replace(path, "//foreign.invalid")) }
        try {
            val repo = createCollectionsRepository(client, client, "https://fixture.invalid", Account())
            assertTrue(repo.create("Title", listOf(roaster, roaster)).isFailure)
            assertTrue(repo.create("Title", emptyList()).isFailure)
            assertEquals(0, requests)
            assertTrue(repo.read(id).isFailure)
        } finally { client.close() }
    }

    @Test fun publicReadUsesOnlyAnonymousClientAndNeverLoadsAccount() = runTest {
        val account = Account()
        val private = client { error("Private client must not handle public collections") }
        val public = client { request ->
            assertEquals("/api/v1/favorite-collections/${id.replace("-", "")}", request.url.encodedPath)
            assertNull(request.headers[HttpHeaders.Authorization])
            respond(body)
        }
        try {
            assertTrue(createCollectionsRepository(private, public, "https://fixture.invalid", account).read(id).isSuccess)
            assertEquals(0, account.loads)
        } finally { private.close(); public.close() }
    }

    @Test fun replacedAccountCannotReceiveCompletionOfEarlierCreate() = runTest {
        val account = Account()
        val client = client { account.owner.value = "second"; respond(body) }
        try {
            assertFailsWith<CancellationException> {
                createCollectionsRepository(client, client, "https://fixture.invalid", account).create("Title", listOf(roaster))
            }
        } finally { client.close() }
    }

    @Test fun cancellationRemainsCancellationAndRevocationAcceptsAlreadyUnavailableLink() = runTest {
        val cancelled = client { throw CancellationException("Cancelled") }
        val deleted = client { respond("", HttpStatusCode.NotFound) }
        try {
            assertFailsWith<CancellationException> { createCollectionsRepository(cancelled, cancelled, "https://fixture.invalid", Account()).read(id) }
            assertTrue(createCollectionsRepository(deleted, deleted, "https://fixture.invalid", Account()).revoke(id).isSuccess)
        } finally { cancelled.close(); deleted.close() }
    }

    private fun client(handler: suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        HttpClient(MockEngine { request -> handler(request) }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    private fun MockRequestHandleScope.respond(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
}
