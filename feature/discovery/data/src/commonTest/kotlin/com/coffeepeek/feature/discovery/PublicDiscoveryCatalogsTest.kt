package com.coffeepeek.feature.discovery

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.discovery.api.PublicCatalogOption
import com.coffeepeek.feature.discovery.api.PublicDiscoveryCatalogs
import com.coffeepeek.feature.discovery.data.repository.createPublicDiscoveryCatalogs
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicDiscoveryCatalogsTest {
    private val paths = mutableListOf<String>()
    private var failing = false
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun catalogs(): PublicDiscoveryCatalogs = createPublicDiscoveryCatalogs(HttpClientFactory(MockEngine { request ->
        paths += request.url.encodedPath
        if (failing) return@MockEngine respond("{}", HttpStatusCode.InternalServerError, json)
        val data = when (request.url.encodedPath) {
            "/api/Catalogs/cities" -> """[{"address":{"slug":"minsk"},"name":"Минск"},{"address":{"slug":"brest"},"name":"Брест"}]"""
            "/api/Catalogs/brew-methods" -> """[{"slug":"v60","name":"V60"},{"slug":"x","address":{"slug":"public-x"},"name":"Икс"},{"slug":"aero"}]"""
            else -> "[]"
        }
        respond("""{"isSuccess":true,"data":$data}""", HttpStatusCode.OK, json)
    }).api("https://api.example"))

    @Test fun theCitiesAreKeyedByTheirPublicSlugAndKept() = runBlocking {
        val source = catalogs()
        val expected = listOf(PublicCatalogOption("minsk", "Минск"), PublicCatalogOption("brest", "Брест"))
        assertEquals(expected, source.cities().getOrThrow())
        assertEquals(expected, source.cities().getOrThrow())
        assertEquals(1, paths.count { it == "/api/Catalogs/cities" })
    }

    @Test fun theBrewMethodsPreferThePublicAddressSlugAndTolerateAMissingName() = runBlocking {
        val methods = catalogs().brewMethods().getOrThrow()
        assertEquals(listOf(PublicCatalogOption("v60", "V60"), PublicCatalogOption("public-x", "Икс"), PublicCatalogOption("aero", "")), methods)
    }

    @Test fun aFailureIsPassedOnAndTheNextCallReadsAgain() = runBlocking {
        val source = catalogs()
        failing = true
        assertTrue(source.cities().isFailure)
        failing = false
        assertEquals(2, source.cities().getOrThrow().size)
        assertEquals(2, paths.count { it == "/api/Catalogs/cities" })
    }
}
