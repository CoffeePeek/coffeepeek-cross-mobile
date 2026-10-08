package com.coffeepeek.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicHttpCacheTest {
    @Test
    fun factoryDoesNotEnableCacheByDefault(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("public", headers = headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            repeat(2) { client.get("/items").bodyAsText() }
            assertEquals(2, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun cookieSettingResponsesAreNeverPersisted(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("session", headers = headersOf(
                HttpHeaders.CacheControl to listOf("public, max-age=3600"),
                HttpHeaders.SetCookie to listOf("session=secret"),
            ))
        }
        val storage = CacheStorage.Unlimited()
        val client = HttpClientFactory(engine).api("https://example.com") { configurePublicHttpCache(storage) }
        try {
            repeat(2) { client.get("/items").bodyAsText() }
            assertEquals(2, requests)
            assertTrue(storage.findAll(io.ktor.http.Url("https://example.com/items")).isEmpty())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun freshPublicResponseUsesInjectedStorage(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("cached", headers = headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
        }
        val storage = CacheStorage.Unlimited()
        val client = HttpClientFactory(engine).api("https://example.com") { configurePublicHttpCache(storage) }
        try {
            repeat(2) { assertEquals("cached", client.get("/items").bodyAsText()) }
            assertEquals(1, requests)
            assertEquals(1, storage.findAll(io.ktor.http.Url("https://example.com/items")).size)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun privateAndNoStoreResponsesAreNotCached(): Unit = runBlocking {
        for (directive in listOf("private, max-age=3600", "no-store")) {
            var requests = 0
            val engine = MockEngine {
                requests++
                respond("fresh", headers = headersOf(HttpHeaders.CacheControl, directive))
            }
            val client = HttpClientFactory(engine).api("https://example.com") {
                configurePublicHttpCache(CacheStorage.Unlimited())
            }
            try {
                repeat(2) { client.get("/items").bodyAsText() }
                assertEquals(2, requests)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun staleResponseRevalidatesWithEtag(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            if (requests == 1) respond("cached", headers = headersOf(
                HttpHeaders.CacheControl to listOf("public, max-age=0"),
                HttpHeaders.ETag to listOf("version-one"),
            )) else {
                assertEquals("version-one", request.headers[HttpHeaders.IfNoneMatch])
                respond("", HttpStatusCode.NotModified, headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
            }
        }
        val client = HttpClientFactory(engine).api("https://example.com") {
            configurePublicHttpCache(CacheStorage.Unlimited())
        }
        try {
            repeat(2) { assertEquals("cached", client.get("/items").bodyAsText()) }
            assertEquals(2, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun credentialsAreRejectedEvenWhenPublicResponseIsAlreadyCached(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("public", headers = headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
        }
        val client = HttpClientFactory(engine).api("https://example.com") {
            configurePublicHttpCache(CacheStorage.Unlimited())
        }
        try {
            client.get("/items").bodyAsText()
            for (header in listOf(HttpHeaders.Authorization, HttpHeaders.Cookie)) {
                assertTrue(requestResult { client.get("/items") { header(header, "secret") } }.isFailure)
            }
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun cookiesAddedByCookiePluginCannotReadPublicCache(): Unit = runBlocking {
        var requests = 0
        var includeCookie = false
        val cookies = object : CookiesStorage {
            override suspend fun get(requestUrl: Url): List<Cookie> =
                if (includeCookie) listOf(Cookie("session", "secret")) else emptyList()

            override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = Unit
            override fun close() = Unit
        }
        val cacheStorage = CacheStorage.Unlimited()
        val engine = MockEngine {
            requests++
            respond("public", headers = headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
        }
        val client = HttpClientFactory(engine).api("https://example.com") {
            install(HttpCookies) { storage = cookies }
            configurePublicHttpCache(cacheStorage)
        }

        try {
            assertEquals("public", client.get("/items").bodyAsText())
            assertEquals(1, cacheStorage.findAll(io.ktor.http.Url("https://example.com/items")).size)

            includeCookie = true
            assertTrue(requestResult { client.get("/items").bodyAsText() }.isFailure)
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun credentialsInOutgoingContentHeadersAreRejectedBeforeCacheLookup(): Unit = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("public", headers = headersOf(HttpHeaders.CacheControl, "public, max-age=3600"))
        }
        val client = HttpClientFactory(engine).api("https://example.com") {
            configurePublicHttpCache(CacheStorage.Unlimited())
        }

        try {
            val result = requestResult {
                client.get("/items") {
                    setBody(object : OutgoingContent.NoContent() {
                        override val headers = headersOf(HttpHeaders.Cookie, "session=secret")
                    })
                }.bodyAsText()
            }
            assertTrue(result.isFailure)
            assertEquals(0, requests)
        } finally { client.close(); engine.close() }
    }
}
