package com.coffeepeek.feature.homecheckin.data.repository

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.*

class HomePreparationErrorsTest {
    private val draft = HomePreparationInput("2026-09-20T10:00:00Z")

    @Test fun serverFieldKeysAndLegacyMessagesBecomeTypedIssues() = runBlocking {
        val error = failure(HttpStatusCode.BadRequest, """{
            "errorCode":"VALIDATION_FAILED","message":"Validation failed",
            "errors":{"DoseGrams":["DoseGrams must be between 0.01 and 10000 with at most two decimal places."],
            "BrewMethodCode":["Brew method not found in the catalog."],
            "BrewDurationSeconds":["BrewDurationSeconds must be between 1 and 604800."]}}""")
        assertEquals(InvalidInput, error.issue)
        assertEquals(mapOf(Dose to InvalidDose, Method to InvalidMethod, Duration to InvalidDuration), error.fieldErrors)
        assertNull(error.cause)
        assertFalse(error.message.orEmpty().contains("Validation failed"))
    }

    @Test fun theHttpStatusStaysOnTheTypedFailureForDiagnostics() = runBlocking {
        assertEquals(401, failure(HttpStatusCode.Unauthorized, "{}").statusCode)
        assertEquals(429, failure(HttpStatusCode.TooManyRequests, "").statusCode)
        assertEquals(503, failure(HttpStatusCode.ServiceUnavailable, "oops").statusCode)
        assertEquals(404, failure(HttpStatusCode.NotFound, "{}").statusCode)
        assertEquals(400, failure(HttpStatusCode.BadRequest, """{"errors":{"DoseGrams":["x"]}}""").statusCode)
    }

    @Test fun modelBindingPathsMapToFieldsWithoutRawServerMessages() = runBlocking {
        val error = failure(HttpStatusCode.BadRequest, """{"errors":{
            "$.doseGrams":["The JSON value could not be converted to Decimal."],
            "request.Photos[0].StorageKey":["The StorageKey field is required."]}}""")
        assertEquals(mapOf(Dose to InvalidDose, Photos to InvalidPhotos), error.fieldErrors)
    }

    @Test fun messageOnlyCompatibilityRetainsSpecificReasons() = runBlocking {
        for ((status, message, expected) in listOf(
            Triple(HttpStatusCode.BadRequest, "Brew method not found in the catalog.", Method to InvalidMethod),
            Triple(HttpStatusCode.NotFound, "Published coffee product not found.", Bean to BeanUnavailable),
            Triple(HttpStatusCode.Conflict, "The uploads already belong to a committed check-in.", Photos to PhotosAlreadyUsed),
            Triple(HttpStatusCode.OK, "BrewDurationSeconds must be between 1 and 604800.", Duration to InvalidDuration),
        )) {
            val error = failure(status, """{"isSuccess":false,"message":"$message"}""")
            assertEquals(mapOf(expected), error.fieldErrors)
        }
    }

    @Test fun infrastructureStatusWinsOverFieldErrorsAndNeverLeaksPayloadOrCause() = runBlocking {
        for ((status, issue) in listOf(
            HttpStatusCode.Unauthorized to Unauthorized, HttpStatusCode.Forbidden to Forbidden,
            HttpStatusCode.TooManyRequests to RateLimited, HttpStatusCode.ServiceUnavailable to Unavailable,
            HttpStatusCode.NotFound to NotFound, HttpStatusCode.Conflict to Conflict,
        )) {
            val error = failure(status, """{"message":"SQL password=secret"}""")
            assertEquals(issue, error.issue)
            assertTrue(error.fieldErrors.isEmpty())
            assertFalse(error.message.orEmpty().contains("secret"))
            assertNull(error.cause)
        }
        val server = failure(HttpStatusCode.InternalServerError, """{"errors":{"DoseGrams":["SQL secret"]}}""")
        assertEquals(Unavailable, server.issue)
        assertTrue(server.fieldErrors.isEmpty())
    }

    @Test fun malformedAndMissingSuccessPayloadsAreFailures() = runBlocking {
        for (body in listOf("not-json", "[]", """{"data":{}}""", """{"isSuccess":true,"data":null}""",
            """{"isSuccess":true,"data":{"id":"partial"}}""")) {
            val error = failure(HttpStatusCode.OK, body)
            assertEquals(InvalidResponse, error.issue, body)
            assertNull(error.cause)
        }
    }

    @Test fun offlineAndUnknownErrorsBecomeStableReasons() = runBlocking {
        val gateway = client(MockEngine { throw IOException("Failed https://private-host/secret") })
        val uploader = HttpClient(MockEngine { error("No upload expected") })
        try {
            val error = assertIs<HomePreparationFailure>(
                createHomePreparationRepository(gateway, uploader, "https://gateway.example.com").create(draft).exceptionOrNull())
            assertEquals(Offline, error.issue)
            assertNull(error.cause)
            assertFalse(error.message.orEmpty().contains("private-host"))
        } finally { gateway.close(); uploader.close() }
        assertEquals(InvalidInput, failure(HttpStatusCode.BadRequest, """{"message":"InternalDto secret"}""").issue)
    }

    @Test fun expectSuccessValidatorStillMapsHttpStatusAndFields() = runBlocking {
        val error = failure(HttpStatusCode.BadRequest, """{"errors":{"visibility":["Private or Public"]}}""", true)
        assertEquals(mapOf(Visibility to InvalidVisibility), error.fieldErrors)
    }

    @Test fun cancellationIsRethrownNotReturnedAsAFormError() = runBlocking<Unit> {
        val gateway = client(MockEngine { throw CancellationException("cancelled") })
        val uploader = HttpClient(MockEngine { error("No upload expected") })
        try {
            assertFailsWith<CancellationException> {
                createHomePreparationRepository(gateway, uploader, "https://gateway.example.com").create(draft)
            }
        } finally { gateway.close(); uploader.close() }
    }

    private suspend fun failure(status: HttpStatusCode, body: String, expectSuccess: Boolean = false): HomePreparationFailure {
        val gateway = client(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }, expectSuccess)
        val uploader = HttpClient(MockEngine { error("No upload expected") })
        return try {
            assertIs<HomePreparationFailure>(
                createHomePreparationRepository(gateway, uploader, "https://gateway.example.com").create(draft).exceptionOrNull())
        } finally { gateway.close(); uploader.close() }
    }

    private fun client(engine: MockEngine, success: Boolean = false) = HttpClient(engine) {
        expectSuccess = success
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("https://gateway.example.com") }
    }
}
