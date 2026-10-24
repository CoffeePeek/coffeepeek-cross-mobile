package com.coffeepeek.feature.roaster.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore
import com.coffeepeek.feature.roaster.data.repository.createRoasterRepository
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val json = headersOf(HttpHeaders.ContentType, "application/json")

private const val catalog = """{"isSuccess":true,"data":[
  {"address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":1},"name":"Roast",
   "photoUrl":"https://media.example/logo.svg","coverPhoto":{"urls":{"card":"https://media.example/card.svg"}},
   "tags":[{"slug":"decaf","name":"Декаф","sortOrder":2},{"slug":"light","sortOrder":1},{"slug":"","name":"skip"}],
   "coffeeShopsCount":7,"coffeeProductsCount":"21","availableCoffeeProducts":19.0}
]}"""

private class MemoryStore(var value: String? = null, var failWrites: Boolean = false, var failReads: Boolean = false) : RoasterCatalogStore {
    var writes = 0
    override suspend fun read(): String? { if (failReads) error("disk"); return value }
    override suspend fun write(value: String) { if (failWrites) error("disk"); writes++; this.value = value }
}

class RoasterRepositoryTest {
    @Test fun listMapsSummaryWithLegacyPhotoPriorityTagOrderAndFlexibleCounters() = runTest {
        withRepository(MockEngine { respond(catalog, headers = json) }) { repository, _ ->
            val roaster = repository.getRoasters().getOrThrow().single()
            assertEquals("roast", roaster.slug)
            assertEquals("/roasters/roast", roaster.address?.canonicalPath)
            assertEquals("Roast", roaster.name)
            assertEquals("https://media.example/card.svg", roaster.photoUrl)
            assertEquals(listOf("light", "decaf"), roaster.tags.map { it.slug })
            assertEquals("light", roaster.tags.first().name) // blank name falls back to slug
            assertEquals(Triple(7, 21, 19), Triple(roaster.coffeeShopsCount, roaster.coffeeProductsCount, roaster.availableCoffeeProducts))
        }
    }

    @Test fun photoFallsBackToLogoThenCoverUrlThenStorageKey() = runTest {
        val body = """{"isSuccess":true,"data":[
          {"address":{"slug":"a"},"photoUrl":"https://m/logo"},
          {"address":{"slug":"b"},"coverPhoto":{"fullUrl":"https://m/full"}},
          {"address":{"slug":"c"},"coverPhoto":{"storageKey":"k/1.png"}},
          {"address":{"slug":"d"}}]}"""
        withRepository(MockEngine { respond(body, headers = json) }, files = "https://files.example/") { repository, _ ->
            assertEquals(listOf("https://m/logo", "https://m/full", "https://files.example/api/file/k/1.png", null),
                repository.getRoasters().getOrThrow().map { it.photoUrl })
        }
    }

    @Test fun unaddressedCardsAreKeptAndDuplicateSlugsDropped() = runTest {
        val body = """{"isSuccess":true,"data":[
          {"address":null,"name":"Unaddressed","tags":null},
          {"address":{"slug":null,"canonicalPath":null,"revision":"0"},"name":"Another"},
          {"address":{"slug":"dup"},"name":"First"},{"address":{"slug":"dup"},"name":"Second"}]}"""
        withRepository(MockEngine { respond(body, headers = json) }) { repository, _ ->
            val items = repository.getRoasters().getOrThrow()
            assertEquals(listOf("Unaddressed", "Another", "First"), items.map { it.name })
            assertEquals(listOf<String?>(null, null, "dup"), items.map { it.slug })
        }
    }

    @Test fun listAndDetailUseExistingPathsAndVerbs() = runTest {
        val seen = mutableListOf<Pair<HttpMethod, String>>()
        val engine = MockEngine { request ->
            seen += request.method to request.url.encodedPath
            respond(if (request.url.encodedPath == "/api/roasters") catalog else detail, headers = json)
        }
        withRepository(engine) { repository, _ ->
            repository.getRoasters().getOrThrow()
            repository.getRoaster("roast").getOrThrow()
            repository.getRoaster("with space/slash").getOrThrow()
        }
        assertEquals(listOf(HttpMethod.Get to "/api/roasters", HttpMethod.Get to "/api/roasters/roast",
            HttpMethod.Get to "/api/roasters/with%20space%2Fslash"), seen)
    }

    @Test fun detailMapsLocationContactPhotosAndShopsLikeLegacy() = runTest {
        withRepository(MockEngine { respond(detail, headers = json) }, files = "https://files.example") { repository, _ ->
            val details = repository.getRoaster("roast").getOrThrow()
            assertEquals("roast", details.id)
            assertEquals("Roast", details.name)
            assertEquals("Улица, 1", details.location?.address)
            assertEquals(53.9, details.location?.latitude)
            assertEquals("https://insta", details.contact?.instagramLink)
            assertNull(details.contact?.siteLink) // blank link dropped
            assertEquals(listOf("p1", "p2"), details.photos.map { it.id }) // sorted by sortIndex
            assertEquals(listOf("https://files.example/api/file/key-1", "https://media/p2"), details.photos.map { it.url })
            val shop = details.shops.single()
            assertEquals("cafe", shop.slug); assertEquals("https://media/shop", shop.photoUrl)
        }
    }

    @Test fun detailWithoutLocationAddressOrUnresolvablePhotosStaysValid() = runTest {
        val body = """{"isSuccess":true,"data":{"address":{"slug":"r"},"name":"R","location":{"address":" "},
          "photos":[{"id":"x","storageKey":"","fullUrl":null}]}}"""
        withRepository(MockEngine { respond(body, headers = json) }) { repository, _ ->
            val details = repository.getRoaster("r").getOrThrow()
            assertNull(details.location); assertNull(details.contact)
            assertTrue(details.photos.isEmpty() && details.shops.isEmpty())
        }
    }

    @Test fun httpEnvelopeAndDecodeFailuresAreResultsAndBlankSlugIsRejected() = runTest {
        withRepository(MockEngine { respond("""{"isSuccess":false,"message":"Nope","data":null}""", HttpStatusCode.OK, json) }) { r, _ ->
            assertEquals("Nope", r.getRoasters().exceptionOrNull()?.message)
            assertTrue(r.getRoaster("x").isFailure)
        }
        withRepository(MockEngine { respond("""{"isSuccess":true,"data":null}""", HttpStatusCode.OK, json) }) { r, _ -> assertTrue(r.getRoaster("x").isFailure) }
        withRepository(MockEngine { respond("""{"isSuccess":true,"data":[]}""", HttpStatusCode.InternalServerError, json) }) { r, _ -> assertTrue(r.getRoaster("x").isFailure) }
        withRepository(MockEngine { respond("not json", HttpStatusCode.OK, json) }) { r, _ -> assertTrue(r.getRoaster("x").isFailure) }
        withRepository(MockEngine { respond(detail, headers = json) }) { r, _ ->
            assertTrue(r.getRoaster(" ").isFailure)
            assertTrue(r.getRoaster("x".repeat(201)).isFailure)
        }
    }

    @Test fun anExplicitRefreshAsksTheServerAgainOnTheSameRepositoryAndKeepsTheDataWhenItFails() = runTest {
        var clock = 1_000L
        var requests = 0
        var mode = "v1"
        val engine = MockEngine {
            requests++
            when (mode) {
                "v1" -> respond(catalog, headers = json)
                "v2" -> respond(catalog.replace("\"name\":\"Roast\"", "\"name\":\"Roast v2\""), headers = json)
                else -> respond("""{"isSuccess":false}""", HttpStatusCode.ServiceUnavailable, json)
            }
        }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try {
            val repository = createRoasterRepository(client, "https://files.example", MemoryStore()) { clock }
            assertEquals("Roast", repository.getRoasters().getOrThrow().single().name)
            mode = "v2"
            // A normal read stays stable, also after the saved copy expired; only the refresh reads the server.
            clock += 25 * 60 * 60 * 1_000L
            assertEquals("Roast", repository.getRoasters().getOrThrow().single().name); assertEquals(1, requests)
            assertEquals("Roast v2", repository.refreshRoasters().getOrThrow().single().name); assertEquals(2, requests)
            assertEquals("Roast v2", repository.getRoasters().getOrThrow().single().name); assertEquals(2, requests)
            mode = "down"
            assertEquals("Roast v2", repository.refreshRoasters().getOrThrow().single().name) // a failed refresh keeps what is loaded
            assertEquals("Roast v2", repository.getRoasters().getOrThrow().single().name)
        } finally { client.close(); engine.close() }
    }

    @Test fun expiredSavedDataShownOfflineIsReplacedByTheRefreshOnceTheServerIsBack() = runTest {
        var clock = 1_000L
        var offline = false
        val store = MemoryStore()
        val engine = MockEngine {
            if (offline) respond("""{"isSuccess":false}""", HttpStatusCode.ServiceUnavailable, json)
            else respond(catalog.replace("\"name\":\"Roast\"", "\"name\":\"Roast v2\""), headers = json)
        }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try {
            // Saved long ago under the old name; the server is down when the session starts.
            createRoasterRepository(client, "https://files.example", store) { clock }.getRoasters().getOrThrow()
            store.value = store.value!!.replace("Roast v2", "Roast")
            clock += 25 * 60 * 60 * 1_000L; offline = true
            val repository = createRoasterRepository(client, "https://files.example", store) { clock }
            assertEquals("Roast", repository.getRoasters().getOrThrow().single().name)
            offline = false
            assertEquals("Roast", repository.getRoasters().getOrThrow().single().name) // the normal read is the loaded session
            assertEquals("Roast v2", repository.refreshRoasters().getOrThrow().single().name)
        } finally { client.close(); engine.close() }
    }

    @Test fun concurrentLoadsShareOneRequestAndLoadedSessionStaysStable() = runTest {
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val engine = MockEngine { requests++; release.await(); respond(catalog, headers = json) }
        withRepository(engine) { repository, _ ->
            val first = async { repository.getRoasters().getOrThrow() }
            val second = async { repository.getRoasters().getOrThrow() }
            release.complete(Unit)
            assertEquals(first.await(), second.await())
            repeat(3) { assertEquals(first.await(), repository.getRoasters().getOrThrow()) }
            assertEquals(1, requests)
        }
    }

    @Test fun freshDiskCacheIsReusedExpiredIsRefreshedAndOfflineFallsBackToExpired() = runTest {
        var clock = 1_000L
        var requests = 0
        var offline = false
        val store = MemoryStore()
        val engine = MockEngine {
            requests++
            if (offline) respond("""{"isSuccess":false}""", HttpStatusCode.ServiceUnavailable, json) else respond(catalog, headers = json)
        }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        fun repo() = createRoasterRepository(client, "https://files.example", store) { clock }
        try {
            val expected = repo().getRoasters().getOrThrow()
            clock += 1_000
            assertEquals(expected, repo().getRoasters().getOrThrow()); assertEquals(1, requests)
            clock += 25 * 60 * 60 * 1_000L
            assertEquals(expected, repo().getRoasters().getOrThrow()); assertEquals(2, requests)
            clock += 25 * 60 * 60 * 1_000L; offline = true
            assertEquals(expected, repo().getRoasters().getOrThrow()) // expired saved data beats an error
            assertTrue(createRoasterRepository(client, "https://files.example", MemoryStore()) { clock }.getRoasters().isFailure)
        } finally { client.close(); engine.close() }
    }

    @Test fun legacyShapedSavedCatalogIsReadAndStoreFailuresNeverHideData() = runTest {
        val saved = """{"savedAtMs":1000,"items":[{"address":{"slug":"old","canonicalPath":"/roasters/old","revision":0,"isAlias":false},
          "name":"Old","photoUrl":null,"coverPhoto":null,"tags":null,"coffeeShopsCount":1,"coffeeProductsCount":2,"availableCoffeeProducts":3}]}"""
        var requests = 0
        val engine = MockEngine { requests++; respond(catalog, headers = json) }
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try {
            val fromDisk = createRoasterRepository(client, "https://f", MemoryStore(saved)) { 2_000 }.getRoasters().getOrThrow()
            assertEquals(listOf("old"), fromDisk.map { it.slug }); assertEquals(0, requests)
            val broken = createRoasterRepository(client, "https://f", MemoryStore(failWrites = true, failReads = true)) { 2_000 }
            assertEquals(listOf("roast"), broken.getRoasters().getOrThrow().map { it.slug })
            assertEquals(1, requests)
            val corrupt = MemoryStore("{broken")
            assertEquals(listOf("roast"), createRoasterRepository(client, "https://f", corrupt) { 2_000 }.getRoasters().getOrThrow().map { it.slug })
            assertTrue(corrupt.value!!.contains("\"savedAtMs\":2000"))
        } finally { client.close(); engine.close() }
    }

    @Test fun validEmptyCatalogIsCachedInMemory() = runTest {
        var requests = 0
        val engine = MockEngine { requests++; respond("""{"isSuccess":true,"data":[]}""", headers = json) }
        withRepository(engine) { repository, _ ->
            repeat(2) { assertTrue(repository.getRoasters().getOrThrow().isEmpty()) }
            assertEquals(1, requests)
        }
    }

    @Test fun cancellationIsRethrownNotWrappedAsResult() = runTest {
        val engine = MockEngine { throw CancellationException("stop") }
        withRepository(engine) { repository, _ ->
            assertFailsWith<CancellationException> { repository.getRoasters() }
            assertFailsWith<CancellationException> { repository.getRoaster("roast") }
        }
    }

    private suspend fun withRepository(
        engine: MockEngine,
        files: String = "https://files.example",
        store: MemoryStore = MemoryStore(),
        block: suspend (RoasterRepository, HttpClient) -> Unit,
    ) {
        val client = HttpClientFactory(engine).api("https://example.invalid")
        try { block(createRoasterRepository(client, files, store) { 1_000 }, client) } finally { client.close(); engine.close() }
    }
}

private const val detail = """{"isSuccess":true,"data":{
  "address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":2},"name":"Roast","about":"About",
  "location":{"address":"Улица, 1","latitude":53.9,"longitude":27.5},
  "contact":{"instagramLink":"https://insta","siteLink":"  "},
  "photos":[{"id":"p2","storageKey":"key-2","fullUrl":"https://media/p2","sortIndex":2},{"id":"p1","storageKey":"key-1","sortIndex":1}],
  "shops":[{"address":{"slug":"cafe","canonicalPath":"/coffee-shops/cafe"},"name":"Cafe","coverPhoto":{"fullUrl":"https://media/shop"}}]}}"""
