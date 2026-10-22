package com.coffeepeek.api.service

import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UsernameUpdateFailureTest {
    @Test fun aRejectedUsernamePreservesTheStatusAndServerMessageForTheAdapter() = runBlocking {
        val client = client("""{"isSuccess":false,"message":"Username change limit reached"}""", HttpStatusCode.TooManyRequests)
        try {
            val failure = assertIs<ApiException>(UserApiService(client).updateUsername("new_name").exceptionOrNull())
            assertEquals(429, failure.statusCode)
            assertEquals("Username change limit reached", failure.message)
        } finally { client.close() }
    }

    @Test fun aNonJsonErrorStillPreservesTheStatus() = runBlocking {
        val client = client("unavailable", HttpStatusCode.ServiceUnavailable)
        try {
            val failure = assertIs<ApiException>(UserApiService(client).updateUsername("new_name").exceptionOrNull())
            assertEquals(503, failure.statusCode)
        } finally { client.close() }
    }

    private fun client(body: String, status: HttpStatusCode) = HttpClient(MockEngine { request ->
        assertEquals("/api/Users/me/username", request.url.encodedPath)
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(JsonExt.json) } }
}
