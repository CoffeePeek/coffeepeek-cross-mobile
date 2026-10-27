package com.coffeepeek.feature.notifications.data

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class HttpNotificationRepositoryTest {
    @Test fun mobileContractLoadsTypedHistoryAndWritesExplicitPushAndReadSnapshot(): Unit = runBlocking {
        val requests = mutableListOf<String>()
        val headers = headersOf(HttpHeaders.ContentType, "application/json")
        val client = HttpClientFactory(MockEngine { request ->
            requests += "${request.method.value} ${request.url.encodedPath}"
            when {
                request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/push") -> respond("""{"enabled":false}""", headers = headers)
                request.method == HttpMethod.Get -> {
                    assertEquals("30", request.url.parameters["pageSize"])
                    assertEquals("previous", request.url.parameters["before"])
                    respond("""{"items":[{"id":"n","type":"RoasterRejected","title":"Решение модерации","body":"Отклонено","createdAtUtc":"2026-10-21T08:00:00Z","readAtUtc":null,"status":"Rejected","entityType":"Roaster"}],"nextCursor":null,"unreadCount":1}""", headers = headers)
                }
                else -> {
                    val body = (request.body as? TextContent)?.text
                    if (request.url.encodedPath.endsWith("/push")) assertEquals("""{"enabled":false}""", body)
                    if (request.url.encodedPath.endsWith("/notifications/read")) assertEquals("""{"throughId":"n"}""", body)
                    respond("", HttpStatusCode.NoContent)
                }
            }
        }).api("https://api.example")
        try {
            val repository = HttpNotificationRepository(client)
            val page = repository.inbox("previous")
            assertEquals("RoasterRejected", page.items.single().type)
            assertEquals("Roaster", page.items.single().entityType)
            assertNull(page.items.single().readAtUtc)
            assertFalse(repository.pushEnabled())
            repository.setPushEnabled(false)
            repository.markRead("n")
            repository.markAllRead("n")
            assertEquals(listOf("GET /api/v1/me/notifications", "GET /api/v1/me/notifications/push",
                "PUT /api/v1/me/notifications/push", "PUT /api/v1/me/notifications/n/read", "PUT /api/v1/me/notifications/read"), requests)
        } finally { client.close() }
    }

    @Test fun failedSettingsWritesAreNotReportedAsSuccess(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }).api("https://api.example")
        try { assertFails { HttpNotificationRepository(client).setPushEnabled(false) } }
        finally { client.close() }
    }

    @Test fun destinationUsesTheOwnedNotificationEndpointAndPreservesItsCurrentPublicSlug(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/v1/me/notifications/notice/destination", request.url.encodedPath)
            respond("""{"kind":"Shop","value":"renamed-cafe"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }).api("https://api.example")
        try {
            assertEquals(com.coffeepeek.feature.notifications.domain.NotificationTarget("Shop", "renamed-cafe"),
                HttpNotificationRepository(client).destination("notice"))
        } finally { client.close() }
    }

    @Test fun noDestinationRemainsDifferentFromAnUnavailableOrForeignTarget(): Unit = runBlocking {
        var status = HttpStatusCode.NoContent
        val client = HttpClientFactory(MockEngine { respond("", status) }).api("https://api.example")
        try {
            val repository = HttpNotificationRepository(client)
            assertNull(repository.destination("notice"))
            status = HttpStatusCode.NotFound
            assertFails { repository.destination("notice") }
            status = HttpStatusCode.ServiceUnavailable
            assertFails { repository.destination("notice") }
        } finally { client.close() }
    }
}
