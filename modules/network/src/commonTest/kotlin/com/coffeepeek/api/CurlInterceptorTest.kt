package com.coffeepeek.api

import com.coffeepeek.api.utils.CurlInterceptor.asCurlString
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurlInterceptorTest {
    @Test fun omitsQueryFragmentAndUserinfo() {
        val request = HttpRequestBuilder().apply {
            url("https://secret-user:secret-pass@example.com/photo?signature=signed-secret&token=query-secret#fragment-secret")
        }
        val curl = request.asCurlString()
        listOf("secret-user", "secret-pass", "signed-secret", "query-secret", "fragment-secret").forEach {
            assertFalse(curl.contains(it), curl)
        }
        assertTrue(curl.contains("example.com:443/photo"))
    }

    @Test
    fun redactsRecoveryTokenAndPasswordsIncludingEscapedQuotes() {
        val request = HttpRequestBuilder().apply {
            setBody(TextContent(
                """{"token":"reset-secret","newPassword":"prefix\"secret-tail","currentPassword":"old-secret","email":"user@example.com"}""",
                ContentType.Application.Json,
            ))
        }
        val curl = request.asCurlString()
        listOf("reset-secret", "prefix", "secret-tail", "old-secret").forEach { assertFalse(curl.contains(it)) }
        assertTrue(curl.contains("user@example.com"))
    }
}
