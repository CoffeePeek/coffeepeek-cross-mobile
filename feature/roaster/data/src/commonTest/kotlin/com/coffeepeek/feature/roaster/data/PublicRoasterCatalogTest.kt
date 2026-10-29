package com.coffeepeek.feature.roaster.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalogEntry
import com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore
import com.coffeepeek.feature.roaster.data.repository.createPublicRoasterCatalog
import com.coffeepeek.feature.roaster.data.repository.createRoasterRepository
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PublicRoasterCatalogTest {
    private val payload = """{"isSuccess":true,"data":[
        {"address":{"slug":"roast"},"name":"Roast","coverPhoto":{"urls":{"card":"card"}}},
        {"name":"No public address"},{"address":{"slug":"roast"},"name":"Duplicate"}]}"""

    @Test fun filterChoicesReuseTheScreenRepositoryCacheWithoutAnotherRequest() = runTest {
        var calls = 0
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/roasters", request.url.encodedPath)
            calls++
            respond(payload, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }).api("https://api.example")
        val store = Store()
        try {
            val repository = createRoasterRepository(client, "https://api.example", store) { 1_000 }
            repository.getRoasters().getOrThrow()
            assertEquals(listOf(PublicRoasterCatalogEntry("roast", "Roast", "card")), createPublicRoasterCatalog(repository).load().getOrThrow())
            assertEquals(1, calls)
        } finally { client.close() }
    }

    @Test fun persistedCatalogStillWorksOfflineThroughThePublicPort() = runTest {
        val client = HttpClientFactory(MockEngine { error("offline") }).api("https://api.example")
        val store = Store("""{"savedAtMs":0,"items":[{"address":{"slug":"roast"},"name":"Saved","photoUrl":"logo"}]}""")
        try {
            val repository = createRoasterRepository(client, "https://api.example", store) { 100_000_000 }
            assertEquals(listOf(PublicRoasterCatalogEntry("roast", "Saved", "logo")), createPublicRoasterCatalog(repository).load().getOrThrow())
        } finally { client.close() }
    }

    @Test fun failuresAndCancellationAreNotConvertedToAnEmptyCatalog() = runTest {
        val error = IllegalStateException("offline")
        assertSame(error, createPublicRoasterCatalog(repository { Result.failure(error) }).load().exceptionOrNull())
        assertFailsWith<CancellationException> {
            createPublicRoasterCatalog(repository { Result.failure(CancellationException("cancel")) }).load()
        }
    }

    private fun repository(read: suspend () -> Result<List<RoasterSummary>>) = object : RoasterRepository {
        override suspend fun getRoasters() = read()
        override suspend fun getRoaster(slug: String): Result<RoasterDetails> = error("unused")
    }

    private class Store(var value: String? = null) : RoasterCatalogStore {
        override suspend fun read() = value
        override suspend fun write(value: String) { this.value = value }
    }
}
