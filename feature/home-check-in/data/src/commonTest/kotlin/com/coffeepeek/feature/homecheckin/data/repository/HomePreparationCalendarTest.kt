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

class HomePreparationCalendarTest {
    private val range = HomePreparationDateRange("2026-10-01", "2026-10-31")
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private fun row(id: String, date: String = "2026-10-16", cutout: Boolean = false) =
        """{"id":"$id","preparedOn":"$date","preparedAtUtc":"2026-10-16T07:30:00Z","photos":[{"id":"photo","url":"/original/$id"}],"drinkPhoto":${if (cutout) "\"/cutout/$id\"" else "null"}}"""
    private fun gateway(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        defaultRequest { url("https://fixture.invalid") }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test fun monthPagesKeepCountsPreferCutoutsDeduplicateAndFilterCivilDates() = runBlocking {
        val pages = mutableListOf<String>()
        val gateway = gateway(MockEngine { request ->
            assertEquals(range.from, request.url.parameters["from"])
            assertEquals(range.to, request.url.parameters["to"])
            val body = if (request.url.encodedPath.endsWith("/calendar"))
                """[{"preparedOn":"2026-10-16","brewCount":3},{"preparedOn":"2026-10-17","brewCount":1}]"""
            else {
                pages += assertNotNull(request.url.parameters["pageNumber"])
                assertEquals("100", request.url.parameters["pageSize"])
                val rows = if (pages.size == 1) row("one", cutout = true)
                    else listOf(row("one", cutout = true), row("two"), row("outside", "2026-09-30")).joinToString(",")
                """{"items":[$rows],"totalCount":101}"""
            }
            respond("""{"isSuccess":true,"data":$body}""", headers = headers)
        })
        val upload = HttpClient(MockEngine { error("No uploads") })
        try {
            val days = createHomePreparationRepository(gateway, upload, "https://fixture.invalid").calendar(range).getOrThrow()
            assertEquals(listOf(3, 1), days.map { it.brewCount })
            assertEquals(listOf("1", "2"), pages)
            assertEquals(listOf("https://fixture.invalid/cutout/one", "https://fixture.invalid/original/two"),
                days.first().photos.map { it.url })
            assertEquals(listOf(true, false), days.first().photos.map { it.isCutout })
            assertTrue(days.last().photos.isEmpty())
        } finally { gateway.close(); upload.close() }
    }

    @Test fun emptyCalendarDoesNotRequestAnyPhotoPages() = runBlocking {
        var calls = 0
        val gateway = gateway(MockEngine { request ->
            calls++
            assertTrue(request.url.encodedPath.endsWith("/calendar"))
            respond("""{"isSuccess":true,"data":[]}""", headers = headers)
        })
        val upload = HttpClient(MockEngine { error("No uploads") })
        try {
            assertTrue(createHomePreparationRepository(gateway, upload, "https://fixture.invalid").calendar(range).getOrThrow().isEmpty())
            assertEquals(1, calls)
        } finally { gateway.close(); upload.close() }
    }

    @Test fun missingMonthPageFailsInsteadOfPublishingAnIncompleteCalendar() = runBlocking {
        val gateway = gateway(MockEngine { request ->
            val data = if (request.url.encodedPath.endsWith("/calendar"))
                """[{"preparedOn":"2026-10-16","brewCount":2}]"""
                else """{"items":[],"totalCount":101}"""
            respond("""{"isSuccess":true,"data":$data}""", headers = headers)
        })
        val upload = HttpClient(MockEngine { error("No uploads") })
        try {
            val error = assertIs<HomePreparationFailure>(createHomePreparationRepository(gateway, upload, "https://fixture.invalid")
                .calendar(range).exceptionOrNull())
            assertEquals(HomePreparationIssue.InvalidResponse, error.issue)
        } finally { gateway.close(); upload.close() }
    }

    @Test fun invalidMonthPageMetadataDoesNotEscapeAsAPartialSuccess() = runBlocking {
        for (data in listOf("""{"items":[],"totalCount":-1}""",
            """{"items":[${List(101) { row("entry") }.joinToString(",")}],"totalCount":101}""")) {
            val gateway = gateway(MockEngine { request ->
                val body = if (request.url.encodedPath.endsWith("/calendar"))
                    """[{"preparedOn":"2026-10-16","brewCount":1}]""" else data
                respond("""{"isSuccess":true,"data":${body}}""", headers = headers)
            })
            val upload = HttpClient(MockEngine { error("No uploads") })
            try {
                val error = assertIs<HomePreparationFailure>(createHomePreparationRepository(gateway, upload, "https://fixture.invalid")
                    .calendar(range).exceptionOrNull())
                assertEquals(HomePreparationIssue.InvalidResponse, error.issue)
            } finally { gateway.close(); upload.close() }
        }
    }

    @Test fun cancellationDuringPhotoPageEscapesTheResultContract(): Unit = runBlocking {
        val gateway = gateway(MockEngine { request ->
            if (!request.url.encodedPath.endsWith("/calendar")) throw CancellationException("Owner replaced")
            respond("""{"isSuccess":true,"data":[{"preparedOn":"2026-10-16","brewCount":1}]}""", headers = headers)
        })
        val upload = HttpClient(MockEngine { error("No uploads") })
        try {
            assertFailsWith<CancellationException> {
                createHomePreparationRepository(gateway, upload, "https://fixture.invalid").calendar(range)
            }
        } finally { gateway.close(); upload.close() }
    }
}
