package com.coffeepeek.data.repository

import com.coffeepeek.api.service.PhotoApiService
import com.coffeepeek.api.service.RoasterApiService
import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoasterCatalogCacheTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val response = """{"isSuccess":true,"data":[
        {"address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":1},"name":"Roast",
         "photoUrl":"https://media.example/logo.svg","coverPhoto":{"urls":{"card":"https://media.example/card.svg"}},
         "tags":[{"slug":"decaf","name":"Декаф","sortOrder":2}],"coffeeShopsCount":7,"coffeeProductsCount":21,"availableCoffeeProducts":19}
    ]}"""

    @Test
    fun listAndShopFilterCatalogShareOneConcurrentRequestAndMemoryCache() = runBlocking {
        withTimeout(5_000) {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var catalogRequests = 0
            val client = client(MockEngine { request ->
                val body = if (request.url.encodedPath == "/api/roasters") {
                    catalogRequests++
                    started.complete(Unit)
                    release.await()
                    response
                } else {
                    assertTrue(request.url.encodedPath.startsWith("/api/Catalogs/"))
                    assertTrue(!request.url.encodedPath.endsWith("/roasters"))
                    """{"isSuccess":true,"data":[]}"""
                }
                respond(body, headers = headers)
            })
            try {
                val catalog = repository(client, CacheSettings())
                val shops = ShopRepositoryImpl(ShopApiService(client), photos(client), unusedFavorites(), FileUrlResolver("https://api.example"), catalog)
                val first = async { catalog.getRoasters().getOrThrow() }
                started.await()
                val filters = async { shops.getCatalogs().getOrThrow() }
                release.complete(Unit)
                val roaster = first.await().single()
                assertEquals("roast", roaster.publicAddress?.slug)
                assertEquals(7, roaster.coffeeShopsCount)
                assertEquals(21, roaster.coffeeProductsCount)
                assertEquals(19, roaster.availableCoffeeProducts)
                assertEquals("decaf", roaster.tags.single().slug)
                assertEquals("https://media.example/card.svg", roaster.photoUrl)
                assertEquals("roast", filters.await().roasters.single().id)
                repeat(3) { assertEquals(listOf(roaster), catalog.getRoasters().getOrThrow()) }
                assertEquals(1, catalogRequests)
            } finally { client.close() }
        }
    }

    @Test
    fun restoresFreshDiskCacheAndRefreshesExpiredDataOnNextRepositoryInstance() = runBlocking {
        var requests = 0
        var clock = 1_000L
        val settings = CacheSettings()
        val client = client(MockEngine { requests++; respond(response, headers = headers) })
        try {
            val first = repository(client, settings, now = { clock })
            val expected = first.getRoasters().getOrThrow()
            clock += 1_000
            assertEquals(expected, repository(client, settings, now = { clock }).getRoasters().getOrThrow())
            assertEquals(1, requests)
            clock += 25 * 60 * 60 * 1_000L
            // A loaded session remains stable; a new session refreshes expired saved data.
            assertEquals(expected, first.getRoasters().getOrThrow())
            assertEquals(expected, repository(client, settings, now = { clock }).getRoasters().getOrThrow())
            assertEquals(2, requests)
            repository(client, settings, namespace = "https://other.example", now = { clock }).getRoasters().getOrThrow()
            assertEquals(3, requests)
        } finally { client.close() }
    }

    @Test
    fun usesExpiredSavedDataOfflineAndRetriesFailuresWithoutAnyCachedData() = runBlocking {
        var offline = false
        val settings = CacheSettings()
        val client = client(MockEngine {
            if (offline) respond("""{"isSuccess":false,"message":"Offline","data":null}""", HttpStatusCode.ServiceUnavailable, headers)
            else respond(response, headers = headers)
        })
        try {
            val expected = repository(client, settings).getRoasters().getOrThrow()
            offline = true
            assertEquals(expected, repository(client, settings, now = { 25 * 60 * 60 * 1_000L }).getRoasters().getOrThrow())
            val uncached = repository(client, CacheSettings())
            assertTrue(uncached.getRoasters().isFailure)
            offline = false
            assertEquals(expected, uncached.getRoasters().getOrThrow())
        } finally { client.close() }
    }

    @Test
    fun validEmptyCatalogIsCachedAndCacheWriteFailureDoesNotBreakLoading() = runBlocking {
        var requests = 0
        val client = client(MockEngine { requests++; respond("""{"isSuccess":true,"data":[]}""", headers = headers) })
        try {
            val catalog = repository(client, CacheSettings(failWrites = true))
            repeat(2) { assertTrue(catalog.getRoasters().getOrThrow().isEmpty()) }
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test
    fun nullablePublicAddressesDoNotDropUnaddressedCardsOrInventFilterIds() = runBlocking {
        val client = client(MockEngine {
            respond("""{"isSuccess":true,"data":[
                {"address":null,"name":"Unaddressed","photoUrl":null,"coverPhoto":null,"tags":null},
                {"address":{"slug":null,"canonicalPath":null,"revision":"0"},"name":"Another"}
            ]}""", headers = headers)
        })
        try {
            val catalog = repository(client, CacheSettings()).getRoasters().getOrThrow()
            assertEquals(listOf("Unaddressed", "Another"), catalog.map { it.name })
            assertTrue(catalog.all { it.publicAddress == null && it.toCatalogItem().id.isEmpty() })
        } finally { client.close() }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun photos(client: HttpClient) = PhotoRepositoryImpl(PhotoApiService(client, client))

    private fun repository(client: HttpClient, settings: CacheSettings, namespace: String = "https://api.example", now: () -> Long = { 1_000L }) =
        RoasterRepositoryImpl(RoasterApiService(client), photos(client), FileUrlResolver(namespace), settings, namespace, now)

    private fun unusedFavorites() = object : FavoriteRepository {
        override suspend fun getFavoriteIds(): Set<String> = error("Unused")
        override suspend fun isFavorite(shopId: String): Boolean = error("Unused")
        override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("Unused")
        override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Unused")
        override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Unused")
        override suspend fun clearAll(): Unit = error("Unused")
    }
}

private class CacheSettings(private val failWrites: Boolean = false) : SettingRepository {
    private val entries = MutableStateFlow<Map<String, Setting>>(emptyMap())
    override suspend fun read(key: String) = entries.value[key]
    override fun readFlow(key: String) = entries.map { it[key] }
    override suspend fun readAll() = entries.value.values.toList()
    override fun readAllFlow() = entries.map { it.values.toList() }
    override suspend fun save(model: Setting) {
        check(!failWrites) { "Disk unavailable" }
        entries.value = entries.value + (model.key to model)
    }
    override suspend fun delete(key: String) { entries.value = entries.value - key }
}
