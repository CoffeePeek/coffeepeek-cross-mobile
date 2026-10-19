package com.coffeepeek.api.service

import com.coffeepeek.api.installServerErrorValidation
import com.coffeepeek.api.model.request.AppleLoginReq
import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun incompleteSessionHasItsOwnDiagnostic() = runBlocking {
        for (body in listOf(
            """{"isSuccess":true}""",
            """{"isSuccess":true,"data":null}""",
            """{"isSuccess":true,"data":{}}""",
            """{"isSuccess":true,"data":{"accessToken":"access"}}""",
            """{"isSuccess":true,"data":{"refreshToken":"refresh"}}""",
            """{"isSuccess":true,"data":{"accessToken":"","refreshToken":"refresh"}}""",
            """{"isSuccess":true,"data":{"accessToken":"access","refreshToken":" "}}""",
            """{"isSuccess":true,"data":{"accessToken":null,"refreshToken":"refresh"}}""",
        )) {
            val error = assertIs<ApiException>(newClient(body).exchangeFailure())
            assertTrue(error.message.endsWith("[ASI-I]"), error.message)
            assertNull(error.statusCode)
        }
    }

    @Test
    fun rejectedSessionPreservesServerMessageWithApplicationDiagnostic() = runBlocking {
        val error = assertIs<ApiException>(newClient(
            """{"isSuccess":false,"message":"Expired challenge"}""",
        ).exchangeFailure())
        assertEquals("Expired challenge [ASI-A]", error.message)
        assertNull(error.statusCode)
    }

    @Test
    fun clientHttpErrorsPreserveStatusAndServerMessage() = runBlocking {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.TooManyRequests)) {
            val error = assertIs<ApiException>(newClient(
                """{"isSuccess":false,"message":"Apple authorization rejected"}""",
                status,
            ).exchangeFailure())
            assertEquals("Apple authorization rejected [ASI-H${status.value}]", error.message)
            assertEquals(status.value, error.statusCode)
        }
    }

    @Test
    fun nonJsonHttpErrorExposesOnlyStatusAndSafeMessage() = runBlocking {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError)) {
            val error = assertIs<ApiException>(newClient(
                "<html>accessToken=private-access refreshToken=private-refresh</html>",
                status,
                "text/html",
            ).exchangeFailure())
            assertEquals("Сервер отклонил вход через Apple. Попробуйте ещё раз. [ASI-H${status.value}]", error.message)
            assertEquals(status.value, error.statusCode)
            assertNoTokenDetails(error)
        }
    }

    @Test
    fun sharedServerValidatorStatusSurvivesBeforeResponseIsReturned() = runBlocking {
        for (status in listOf(HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable)) {
            val error = assertIs<ApiException>(newClient(
                "<html>accessToken=private-access refreshToken=private-refresh</html>",
                status,
                "text/html",
                validateServerErrors = true,
            ).exchangeFailure())
            assertEquals("Сервис временно недоступен. Попробуйте чуть позже. [ASI-H${status.value}]", error.message)
            assertEquals(status.value, error.statusCode)
            assertNoTokenDetails(error)
        }
    }

    @Test
    fun malformedOrUnexpectedSuccessBodyHasResponseDiagnosticWithoutParserDetails() = runBlocking {
        for (body in listOf(
            """{"accessToken":"private-access","refreshToken":"private-refresh","isSuccess": """,
            """{"isSuccess":true,"data":"private-access"}""",
            """{"isSuccess":true,"data":{"accessToken":[],"refreshToken":"private-refresh"}}""",
            """{"unrecognized":"private-access"}""",
            "[]",
            "null",
        )) {
            val error = assertIs<ApiException>(newClient(body).exchangeFailure())
            assertTrue(error.message.endsWith("[ASI-R]"), error.message)
            assertNull(error.statusCode)
            assertNoTokenDetails(error)
        }
    }

    @Test
    fun htmlSuccessResponseHasResponseDiagnostic() = runBlocking {
        val error = assertIs<ApiException>(newClient(
            "<html>private-access private-refresh</html>",
            contentType = "text/html",
        ).exchangeFailure())
        assertTrue(error.message.endsWith("[ASI-R]"), error.message)
        assertNoTokenDetails(error)
    }

    @Test
    fun pascalCaseEnvelopeRemainsSupportedAndTokensAreNotModified() = runBlocking {
        val client = newClient(
            """{"IsSuccess":true,"Data":{"accessToken":" access ","refreshToken":" refresh "}}""",
        )
        try {
            val response = AuthService(client, client).appleLogin("id", "token").getOrThrow()
            assertEquals(" access ", response.accessToken)
            assertEquals(" refresh ", response.refreshToken)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellationIsRethrownInsteadOfBecomingAResultFailure() = runBlocking {
        val cancellation = CancellationException("cancel Apple exchange")
        val client = HttpClient(MockEngine { throw cancellation }) {
            install(ContentNegotiation) { json(JsonExt.json) }
        }
        // Ktor may copy exceptions while recovering coroutine stack traces.
        val error = assertFailsWith<CancellationException> {
            try { AuthService(client, client).appleLogin("id", "token") }
            finally { client.close() }
        }
        assertEquals(cancellation.message, error.message)
    }

    @Test
    fun requestTimeoutRemainsAvailableForNetworkErrorPresentation() = runBlocking {
        val timeout = HttpRequestTimeoutException("https://example.test/api/Tokens/apple/login", 1_000L)
        val client = HttpClient(MockEngine { throw timeout }) {
            install(ContentNegotiation) { json(JsonExt.json) }
        }
        val error = client.exchangeFailure()
        assertIs<HttpRequestTimeoutException>(error)
        assertEquals(timeout.message, error.message)
        assertTrue(error.message.orEmpty().contains("timeout"))
        assertFalse(error.message.orEmpty().contains("[ASI-R]"))
    }

    @Test
    fun transportFailureWhileReadingBodyIsNotClassifiedAsMalformedJson() = runBlocking {
        val failure = IllegalStateException("network unavailable")
        val client = HttpClient(MockEngine {
            respond(ByteChannel().apply { cancel(failure) }, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(JsonExt.json) } }
        val error = client.exchangeFailure()
        assertFalse(error is ApiException)
        assertEquals("network unavailable", error.message)
        assertFalse(error.message.orEmpty().contains("[ASI-R]"))
    }

    private fun newClient(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: String = "application/json",
        validateServerErrors: Boolean = false,
    ): HttpClient = HttpClient(MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
    }) {
        // Apple exchange must inspect 4xx responses even when a caller enables default validation.
        expectSuccess = true
        install(ContentNegotiation) { json(JsonExt.json) }
        if (validateServerErrors) installServerErrorValidation()
    }

    private suspend fun HttpClient.exchangeFailure(): Throwable = try {
        assertNotNull(AuthService(this, this).appleLogin("id", "token").exceptionOrNull())
    } finally {
        close()
    }

    private fun assertNoTokenDetails(error: Throwable) {
        for (token in listOf("private-access", "private-refresh", "accessToken", "refreshToken", "<html>")) {
            assertFalse(error.message.orEmpty().contains(token), error.message)
        }
        assertNull(error.cause)
    }
}
