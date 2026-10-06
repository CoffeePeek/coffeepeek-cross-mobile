package com.coffeepeek.api.service

import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewReportContractTest {
    @Test
    fun reportContractAndValidation() = runBlocking {
        var requests = 0
        var status = HttpStatusCode.Created
        val client = HttpClient(MockEngine { request ->
            requests++
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/CoffeeShopReviews/review-id/reports", request.url.encodedPath)
            assertEquals("{\"text\":\"Проблема\"}", (request.body as TextContent).text)
            respond(
                """{"isSuccess":true,"data":{"id":"report-id","reviewId":"review-id"}}""",
                status, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(JsonExt.json) }
        }
        try {
            val api = ReviewApiService(client)
            assertEquals("report-id", api.submitReviewReport("review-id", "  Проблема  ").getOrThrow())
            for (text in listOf("   ", "x".repeat(2001))) {
                assertTrue(api.submitReviewReport("review-id", text).isFailure)
            }
            assertEquals(1, requests)
            for ((code, message) in listOf(
                HttpStatusCode.NotFound to "Отзыв удалён или больше недоступен",
                HttpStatusCode.TooManyRequests to "Слишком много отправок. Попробуйте позже",
                HttpStatusCode.Unauthorized to "Войдите в аккаунт, чтобы отправить жалобу",
            )) {
                status = code
                assertEquals(message, api.submitReviewReport("review-id", "Проблема").exceptionOrNull()?.message)
            }
        } finally {
            client.close()
        }
    }
}
