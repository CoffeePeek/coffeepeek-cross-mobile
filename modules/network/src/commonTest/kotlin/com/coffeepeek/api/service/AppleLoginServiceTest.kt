package com.coffeepeek.api.service

import com.coffeepeek.api.model.request.AppleLoginReq
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

class AppleLoginServiceTest {
    @Test
    fun exchangesNativeAppleTokenWithChallengeAndReadsBothSessionTokens() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            val body = when (request.url.encodedPath) {
                "/api/Tokens/apple/challenge" ->
                    """{"isSuccess":true,"data":{"challengeId":"challenge","nonce":"nonce","expiresAt":"2026-10-13T10:00:00Z"}}"""
                "/api/Tokens/apple/login" -> {
                    assertEquals(AppleLoginReq("challenge", "apple-token"), JsonExt.json.decodeFromString<AppleLoginReq>((request.body as TextContent).text))
                    """{"isSuccess":true,"data":{"accessToken":"access","refreshToken":"refresh"}}"""
                }
                else -> error("Unexpected request")
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        try {
            val service = AuthService(client, client)
            val challenge = service.beginAppleLogin().getOrThrow()
            assertEquals("nonce", challenge.nonce)
            val session = service.appleLogin(challenge.challengeId, "apple-token").getOrThrow()
            assertEquals("access", session.accessToken)
            assertEquals("refresh", session.refreshToken)
        } finally { client.close() }
    }

    @Test
    fun incompleteOrRejectedSessionIsNotAccepted() = runBlocking {
        for (body in listOf(
            """{"isSuccess":false,"message":"Expired challenge"}""",
            """{"isSuccess":true,"data":{"accessToken":"access"}}""",
            """{"isSuccess":true,"data":{"accessToken":"","refreshToken":"refresh"}}""",
        )) {
            val client = HttpClient(MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }) { install(ContentNegotiation) { json(JsonExt.json) } }
            try { assertTrue(AuthService(client, client).appleLogin("id", "token").isFailure) }
            finally { client.close() }
        }
    }
}
