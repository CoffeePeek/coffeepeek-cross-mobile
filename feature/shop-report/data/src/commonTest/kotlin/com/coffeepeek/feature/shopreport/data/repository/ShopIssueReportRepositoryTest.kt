package com.coffeepeek.feature.shopreport.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shopreport.data.backend.BackendShopIssueCategory
import com.coffeepeek.feature.shopreport.data.backend.CreateShopIssueReportRequest
import com.coffeepeek.feature.shopreport.data.mapper.toBackend
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopIssueReportRepositoryTest {
    @Test fun submitsToExpectedEndpointAndMapsEveryCategory() = runBlocking {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://example.com/api/ShopIssueReports", request.url.toString())
            val payload = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(JsonPrimitive("shop-1"), payload["shop"])
            assertFalse("shopId" in payload)
            requests += request.url.toString()
            respond("""{"isSuccess":true,"message":"ok"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopIssueReportRepository(client)
            ShopIssueCategory.entries.forEach { category ->
                assertTrue(repository.submitReport("shop-1", category, null).isSuccess)
            }
            assertEquals(ShopIssueCategory.entries.size, requests.size)
            assertEquals(BackendShopIssueCategory.entries.map { it.name },
                ShopIssueCategory.entries.map { it.toBackend().name })
            val encoded = Json.encodeToString(CreateShopIssueReportRequest(
                "shop-1", ShopIssueCategory.Other.toBackend(), "Broken details"))
            assertTrue(encoded.contains("\"category\":\"Other\""))
            assertTrue(encoded.contains("\"description\":\"Broken details\""))
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun rejectedAndHttpErrorResponsesStayFailures() = runBlocking {
        for ((status, body) in listOf(
            HttpStatusCode.OK to """{"isSuccess":false,"message":"rejected"}""",
            HttpStatusCode.InternalServerError to "server failure",
        )) {
            val engine = MockEngine { respond(body, status,
                headersOf(HttpHeaders.ContentType, "application/json")) }
            val client = HttpClientFactory(engine).api("https://example.com")
            try {
                assertTrue(createShopIssueReportRepository(client)
                    .submitReport("shop-1", ShopIssueCategory.Other, "details").isFailure)
            } finally {
                client.close()
                engine.close()
            }
        }
    }

    @Test fun cancellationIsNeverWrappedInResult(): Unit = runBlocking {
        val engine = MockEngine { throw CancellationException("cancelled") }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            assertFailsWith<CancellationException> {
                createShopIssueReportRepository(client)
                    .submitReport("shop-1", ShopIssueCategory.Other, "details")
            }
        } finally {
            client.close()
            engine.close()
        }
    }
}
