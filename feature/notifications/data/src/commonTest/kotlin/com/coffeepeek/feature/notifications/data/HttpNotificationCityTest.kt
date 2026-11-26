package com.coffeepeek.feature.notifications.data

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HttpNotificationCityTest {
    @Test fun citySlugAndExplicitNullUseTheDedicatedAccountContract() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/api/v1/me/notifications/city", request.url.encodedPath)
            bodies += (request.body as TextContent).text
            respond("", HttpStatusCode.NoContent)
        }).api("https://api.example")
        try {
            val repository = HttpNotificationRepository(client)
            repository.setCity("minsk")
            repository.setCity(null)
            assertEquals(listOf("""{"citySlug":"minsk"}""", """{"citySlug":null}"""), bodies)
        } finally { client.close() }
    }

    @Test fun onlyNoContentIsASuccessAndMissingBackendIsNotSilentlyAccepted() = runTest {
        var status = HttpStatusCode.NotFound
        val client = HttpClientFactory(MockEngine { respond("", status) }).api("https://api.example")
        try {
            val repository = HttpNotificationRepository(client)
            for (code in listOf(200, 202, 400, 404, 500, 503)) {
                status = HttpStatusCode.fromValue(code)
                assertFails("HTTP $code") { repository.setCity("minsk") }
            }
            status = HttpStatusCode.NoContent
            repository.setCity("minsk")
        } finally { client.close() }
    }

    @Test fun cityRequestCancellationIsPropagated() = runTest {
        val client = HttpClientFactory(MockEngine { throw CancellationException("cancelled") }).api("https://api.example")
        try {
            assertFailsWith<CancellationException> { HttpNotificationRepository(client).setCity("minsk") }
        } finally { client.close() }
    }
}
