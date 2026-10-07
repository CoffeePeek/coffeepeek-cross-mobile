package com.coffeepeek.api.service

import com.coffeepeek.api.model.request.LoginReq
import com.coffeepeek.api.model.request.RegistrationReq
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

class AuthServiceTest {
    @Test
    fun emailCaseIsIgnoredForLoginRegistrationAndAvailability() = runBlocking {
        val normalizedEmail = "user+tag.name@example.com"
        val password = "  MiXeD-Pass123  "
        val userName = "MiXeD User"
        val emails = listOf(
            normalizedEmail,
            "USER+TAG.NAME@EXAMPLE.COM",
            "UsEr+TaG.NaMe@ExAmPlE.CoM",
            " \tUsEr+TaG.NaMe@ExAmPlE.CoM \n",
        )
        var requests = 0
        val client = HttpClient(MockEngine { request ->
            requests++
            val response = when (request.url.encodedPath) {
                "/api/Tokens" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals(
                        LoginReq(normalizedEmail, password),
                        JsonExt.json.decodeFromString<LoginReq>((request.body as TextContent).text),
                    )
                    """{"isSuccess":true,"data":{"accessToken":"test-access"}}"""
                }
                "/api/Users" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals(
                        RegistrationReq(normalizedEmail, password, userName),
                        JsonExt.json.decodeFromString<RegistrationReq>((request.body as TextContent).text),
                    )
                    """{"isSuccess":true}"""
                }
                "/api/Users/exists" -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals(normalizedEmail, request.url.parameters["email"])
                    """{"isSuccess":true,"data":true}"""
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(JsonExt.json) }
        }
        try {
            val service = AuthService(client, client)
            for (email in emails) {
                assertEquals("test-access", service.login(email, password).getOrThrow().accessToken)
                service.register(userName, email, password).getOrThrow()
                assertTrue(service.isEmailTaken(email).getOrThrow())
            }
            assertEquals(emails.size * 3, requests)
        } finally {
            client.close()
        }
    }
}
