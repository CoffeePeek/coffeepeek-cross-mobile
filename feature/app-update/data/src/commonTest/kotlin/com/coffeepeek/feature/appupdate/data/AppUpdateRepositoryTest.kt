package com.coffeepeek.feature.appupdate.data

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.api.DismissedUpdateStore
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** (Real time: the repository applies a 10 s timeout, which virtual time would trip while the mock engine answers on its own threads.)
 * The HTTP contract and the dismissed-version storage of the update check, as the legacy code implements them. */
class AppUpdateRepositoryTest {
    private class Store : DismissedUpdateStore {
        var stored: Long? = null
        override suspend fun read() = stored
        override suspend fun write(version: Long) { stored = version }
    }
    private class Environment(override val channel: String? = "play") : AppUpdateEnvironment {
        override val versionCode: Long? = 100
        override val platform = "android"
    }

    private val body = """{"isSuccess":true,"data":{"versionCode":140,"minVersionCode":120,"url":"https://example.com/app.apk"}}"""
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(store: Store = Store(), environment: Environment = Environment(), respond: (HttpRequestData) -> Pair<HttpStatusCode, String>, seen: MutableList<HttpRequestData> = mutableListOf()): AppUpdateRepositoryImpl {
        // The shared factory client is what production uses for the unauthenticated update check.
        val client = HttpClientFactory(MockEngine { request ->
            seen += request
            val (status, content) = respond(request)
            respond(content, status, json)
        }).api("https://api.example")
        return AppUpdateRepositoryImpl(client, environment, store)
    }

    @Test fun theRequestAsksTheBackendForThisPlatformAndChannelWithoutCaching() = runBlocking<Unit> {
        val seen = mutableListOf<HttpRequestData>()
        val update = repository(respond = { HttpStatusCode.OK to body }, seen = seen).fetch()
        assertEquals(AppUpdate(140, 120, "https://example.com/app.apk"), update)
        val request = seen.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/app-version", request.url.encodedPath)
        assertEquals("android", request.url.parameters["platform"])
        assertEquals("play", request.url.parameters["channel"])
        assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
    }

    @Test fun noChannelIsSentWhenTheEnvironmentHasNone() = runBlocking<Unit> {
        val seen = mutableListOf<HttpRequestData>()
        repository(environment = Environment(channel = null), respond = { HttpStatusCode.OK to body }, seen = seen).fetch()
        assertNull(seen.single().url.parameters["channel"])
    }

    @Test fun anythingButAValidSuccessfulPolicyIsNoUpdate() = runBlocking<Unit> {
        assertNull(repository(respond = { HttpStatusCode.NoContent to "" }).fetch())
        assertNull(repository(respond = { HttpStatusCode.OK to """{"isSuccess":false,"data":null}""" }).fetch())
        assertNull(repository(respond = { HttpStatusCode.OK to body.replace("https://example.com", "http://example.com") }).fetch())
    }

    @Test fun errorStatusesAndUnreadableBodiesAreErrorsTheStateTurnsIntoAFailedCheck() = runBlocking<Unit> {
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.NotFound to body }).fetch() }
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.ServiceUnavailable to "" }).fetch() }
        assertFailsWith<Exception> { repository(respond = { HttpStatusCode.OK to "not json" }).fetch() }
    }

    @Test fun theDismissedVersionGoesThroughTheStore() = runBlocking<Unit> {
        val store = Store()
        val repository = repository(store, respond = { HttpStatusCode.OK to body })
        assertNull(repository.dismissedVersion())
        repository.dismiss(140)
        assertEquals(140L, store.stored)
        assertEquals(140L, repository.dismissedVersion())
    }
}
