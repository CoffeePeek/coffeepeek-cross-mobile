package com.coffeepeek.feature.homecheckin.data.repository

import com.coffeepeek.feature.homecheckin.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class HomePreparationSafetyTest {
    private val id = "11111111-1111-4111-8111-111111111111"
    private val draft = HomePreparationInput("2026-09-20T10:00:00Z")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun inputValidationAndPageBoundsStopBeforeAnyRequest() = runBlocking {
        var requests = 0
        val gateway = gateway(MockEngine { requests++; error("Unexpected request") })
        val uploader = HttpClient(MockEngine { requests++; error("Unexpected upload") })
        try {
            val repository = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
            assertTrue(repository.create(draft.copy(tasteScore = 6)).isFailure)
            assertTrue(repository.create(draft.copy(photos = listOf(photo(1)),
                drinkPhoto = photo(2).copy(contentType = "image/png", bytes = byteArrayOf()))).isFailure)
            assertTrue(repository.update(id, draft.copy(photos = listOf(photo(1)))).isFailure)
            assertTrue(repository.page(0, 100, null).isFailure)
            assertTrue(repository.page(1, 101, null).isFailure)
            assertTrue(repository.detail("../other").isFailure)
            assertEquals(0, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun sameDayCalendarBoundsRemainInclusiveAndUnchanged() = runBlocking {
        var requests = 0
        val gateway = gateway(MockEngine { request ->
            requests++
            assertEquals("2026-09-20", request.url.parameters["from"])
            assertEquals("2026-09-20", request.url.parameters["to"])
            respond(if (request.url.encodedPath.endsWith("/calendar"))
                """{"isSuccess":true,"data":[{"preparedOn":"2026-09-20","brewCount":2}]}"""
                else """{"isSuccess":true,"data":{"items":[],"totalCount":0}}""", headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { error("Unexpected upload") })
        try {
            assertEquals(2, createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
                .calendar(HomePreparationDateRange("2026-09-20", "2026-09-20")).getOrThrow().single().brewCount)
            assertEquals(2, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun reservationIsValidatedCompletelyBeforeUploadingAnyImage() = runBlocking {
        var uploads = 0
        var requests = 0
        val gateway = gateway(MockEngine {
            requests++
            respond("""{"isSuccess":true,"data":[
                {"uploadUrl":"https://uploads.example.com/one","storageKey":"one"},
                {"uploadUrl":"http://127.0.0.1/private","storageKey":"two"}]}""", headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { uploads++; respond("") })
        try {
            val error = assertIs<HomePreparationFailure>(
                createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
                    .create(draft.copy(photos = listOf(photo(1), photo(2)))).exceptionOrNull())
            assertEquals(HomePreparationOperation.PhotoUpload, error.operation)
            assertEquals(0, uploads)
            assertEquals(1, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun failedUploadNeverCreatesOrAutomaticallyRetries() = runBlocking {
        var gatewayCalls = 0
        var uploadCalls = 0
        val gateway = gateway(MockEngine { request ->
            gatewayCalls++
            assertEquals("/api/Photos/check-in", request.url.encodedPath)
            respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/one","storageKey":"one"}]}""",
                headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { uploadCalls++; respond("", HttpStatusCode.Forbidden) })
        try {
            val error = assertIs<HomePreparationFailure>(createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
                .create(draft.copy(photos = listOf(photo(1)))).exceptionOrNull())
            assertEquals(HomePreparationOperation.PhotoUpload, error.operation)
            assertEquals(1, gatewayCalls)
            assertEquals(1, uploadCalls)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun cancellationDuringSignedPutPropagatesWithoutCreating() = runBlocking<Unit> {
        var requests = 0
        val gateway = gateway(MockEngine {
            requests++
            respond("""{"isSuccess":true,"data":[{"uploadUrl":"https://uploads.example.com/one","storageKey":"one"}]}""",
                headers = jsonHeaders)
        })
        val uploader = HttpClient(MockEngine { throw CancellationException("cancelled upload") })
        try {
            assertFailsWith<CancellationException> {
                createHomePreparationRepository(gateway, uploader, "https://gateway.example.com")
                    .create(draft.copy(photos = listOf(photo(1))))
            }
            assertEquals(1, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun ambiguousCreateResponseDoesNotRepeatTheWrite() = runBlocking {
        var requests = 0
        val gateway = gateway(MockEngine { requests++; respond("incomplete", headers = jsonHeaders) })
        val uploader = HttpClient(MockEngine { error("No upload expected") })
        try {
            val result = createHomePreparationRepository(gateway, uploader, "https://gateway.example.com").create(draft)
            assertTrue(result.isFailure)
            assertEquals(1, requests)
        } finally { gateway.close(); uploader.close() }
    }

    @Test fun gatewayCannotAccidentallyBeUsedAsUploadClient() {
        val client = gateway(MockEngine { error("No request expected") })
        try { assertFailsWith<IllegalArgumentException> {
            createHomePreparationRepository(client, client, "https://gateway.example.com")
        } } finally { client.close() }
    }

    private fun photo(value: Int) = HomePreparationUpload("photo.jpg", "image/jpeg", byteArrayOf(value.toByte()))
    private fun gateway(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        defaultRequest {
            url("https://gateway.example.com")
            headers.append(HttpHeaders.Authorization, "Bearer fixture")
            headers.append(HttpHeaders.Cookie, "session=fixture")
        }
    }
}
