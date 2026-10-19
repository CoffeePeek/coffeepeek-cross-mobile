package com.coffeepeek.admin.feature.passwordrecovery

import com.coffeepeek.admin.feature.passwordrecovery.data.PasswordRecoveryRepositoryImpl
import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasswordRecoveryRepositoryTest {
    @Test
    fun requestsEmailWithExactPublicApiContract(): Unit = runBlocking {
        var requests = 0
        val client = HttpClientFactory(MockEngine { request ->
            requests++
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals("/api/Users/password/forgot", request.url.encodedPath)
            assertEquals(setOf("email"), body.keys)
            assertEquals("user@example.com", body["email"]?.jsonPrimitive?.content)
            respond("""{"isSuccess":true,"message":"OK"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }).api("https://api.coffeepeek.by")
        try {
            val repository = PasswordRecoveryRepositoryImpl(client)
            repository.requestReset(" User@Example.com ").getOrThrow()
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test
    fun handlesHttpAndApplicationFailures(): Unit = runBlocking {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.BadRequest, HttpStatusCode.TooManyRequests, HttpStatusCode.InternalServerError)) {
            val client = HttpClientFactory(MockEngine {
                respond("""{"isSuccess":false,"message":"Rejected"}""", status, headersOf(HttpHeaders.ContentType, "application/json"))
            }).api("https://api.coffeepeek.by")
            try {
                val repository = PasswordRecoveryRepositoryImpl(client)
                assertTrue(repository.requestReset("user@example.com").isFailure)
            } finally { client.close() }
        }
    }

    @Test
    fun preservesRequestCancellation(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { throw CancellationException("Cancelled") }).api("https://api.coffeepeek.by")
        try {
            assertFailsWith<CancellationException> { PasswordRecoveryRepositoryImpl(client).requestReset("user@example.com") }
        } finally { client.close() }
    }
}
