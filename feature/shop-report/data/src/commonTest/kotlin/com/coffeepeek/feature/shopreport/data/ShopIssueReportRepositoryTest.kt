package com.coffeepeek.feature.shopreport.data

import com.coffeepeek.feature.shopreport.data.backend.CreateShopIssueReportRequest
import com.coffeepeek.feature.shopreport.data.mapper.toBackend
import com.coffeepeek.feature.shopreport.data.repository.createShopIssueReportRepository
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShopIssueReportRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    @Test fun submitsToExistingEndpointAndPreservesSuccess() = runBlocking {
        var path: String? = null
        var method: HttpMethod? = null
        val client = client(MockEngine { request ->
            path = request.url.encodedPath
            method = request.method
            respond("""{"isSuccess":true,"entityId":"report-1"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val result = createShopIssueReportRepository(client)
                .submitReport("shop-1", ShopIssueCategory.Other, "Wrong address")
            assertTrue(result.isSuccess)
            assertEquals("/api/ShopIssueReports", path)
            assertEquals(HttpMethod.Post, method)
        } finally {
            client.close()
        }
    }

    @Test fun wireCategoriesMatchLegacyContract() {
        ShopIssueCategory.entries.forEach { category ->
            val encoded = json.encodeToString(CreateShopIssueReportRequest("shop-1", category.toBackend(), "Details"))
            val fields = json.parseToJsonElement(encoded).jsonObject
            assertEquals("shop-1", fields.getValue("shopId").jsonPrimitive.content)
            assertEquals(category.name, fields.getValue("category").jsonPrimitive.content)
            assertEquals("Details", fields.getValue("description").jsonPrimitive.content)
        }
    }

    @Test fun rejectedResponseIsResultFailure() = runBlocking {
        val client = client(MockEngine {
            respond("""{"IsSuccess":false,"Message":"invalid report"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val result = createShopIssueReportRepository(client)
                .submitReport("shop-1", ShopIssueCategory.OutdatedMenu, null)
            assertEquals("invalid report", result.exceptionOrNull()?.message)
        } finally {
            client.close()
        }
    }

    @Test fun cancellationIsNotConvertedToResultFailure(): Unit = runBlocking {
        val client = client(MockEngine { throw CancellationException("cancelled") })
        try {
            assertFailsWith<CancellationException> {
                createShopIssueReportRepository(client)
                    .submitReport("shop-1", ShopIssueCategory.ShopClosed, null)
            }
        } finally {
            client.close()
        }
    }
}
