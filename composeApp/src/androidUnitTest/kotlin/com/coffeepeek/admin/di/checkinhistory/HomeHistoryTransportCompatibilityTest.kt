package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Active migrated transport and cross-feature adapter with fake HTTP; no backend/account. */
class HomeHistoryTransportCompatibilityTest {
    @Test fun actualMigratedTransportKeepsEqualDayBoundsAndCivilCalendarDates() = runTest {
        var calls = 0
        val protectedUrl = "https://fixture.invalid/api/v1/check-ins/owned/photos/p?signature=issued"
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            when (calls++) {
                0 -> {
                    assertEquals("/api/v1/check-ins/home", request.url.encodedPath)
                    assertEquals("2", request.url.parameters["pageNumber"])
                    assertEquals("100", request.url.parameters["pageSize"])
                    assertEquals("2026-10-16", request.url.parameters["from"])
                    assertEquals("2026-10-16", request.url.parameters["to"])
                    respond("""{"isSuccess":true,"data":{"items":[{"id":"home-id",
                        "preparedOn":"2026-10-16","preparedAtUtc":"2026-10-15T22:15:00Z",
                        "doseGrams":75.5,"brewDurationSeconds":210,"tasteScore":4,
                        "visibility":"FutureValue","moderationState":"FutureValue",
                        "photos":[{"id":"p","url":"$protectedUrl"}],"drinkPhoto":{"url":"$protectedUrl"}}],
                        "totalCount":101}}""", headers = headersOf("Content-Type", "application/json"))
                }
                1 -> {
                    assertEquals("/api/v1/check-ins/home/calendar", request.url.encodedPath)
                    assertEquals("2024-02-01", request.url.parameters["from"])
                    assertEquals("2024-02-29", request.url.parameters["to"])
                    respond("""{"isSuccess":true,"data":[{"preparedOn":"2024-02-29","brewCount":2}]}""",
                        headers = headersOf("Content-Type", "application/json"))
                }
                else -> {
                    assertEquals(3, calls)
                    assertEquals("/api/v1/check-ins/home", request.url.encodedPath)
                    assertEquals("1", request.url.parameters["pageNumber"])
                    assertEquals("100", request.url.parameters["pageSize"])
                    assertEquals("2024-02-01", request.url.parameters["from"])
                    assertEquals("2024-02-29", request.url.parameters["to"])
                    respond("""{"isSuccess":true,"data":{"items":[{"id":"month-home",
                        "preparedOn":"2024-02-29","preparedAtUtc":"2024-02-29T08:00:00Z",
                        "drinkPhoto":{"url":"$protectedUrl"}}],"totalCount":1}}""",
                        headers = headersOf("Content-Type", "application/json"))
                }
            }
        }) { defaultRequest { url("https://fixture.invalid") } }
        val uploader = HttpClient(MockEngine { error("Read foundation must not upload") })
        try {
            val reader = HomeHistoryReaderAdapter(createHomePreparationRepository(client, uploader, "https://fixture.invalid"))
            val page = reader.page(2, 100, HomeHistoryDateRange("2026-10-16", "2026-10-16")).getOrThrow()
            val item = page.items.single()
            assertEquals("2026-10-16", item.preparedOn)
            assertEquals("2026-10-15T22:15:00Z", item.preparedAtUtc)
            assertEquals("75.5", item.recipe.doseGrams)
            assertEquals(protectedUrl, item.photos.single().url)
            assertNull(item.visibility)
            assertEquals("Unknown", item.moderation.name)
            assertFalse(page.hasMore)
            val calendarDay = reader.calendar(HomeHistoryDateRange("2024-02-01", "2024-02-29")).getOrThrow().single()
            assertEquals("2024-02-29", calendarDay.preparedOn)
            // An old cutout-only record still contributes visits, never a retired image.
            assertEquals(2, calendarDay.brewCount)
            assertTrue(calendarDay.photos.isEmpty())
            assertEquals(3, calls)
        } finally { client.close(); uploader.close() }
    }

    @Test fun rejectedEnvelopeIsNotTurnedIntoSuccessfulEmptyHistory() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"isSuccess":false,"message":"Denied","data":null}""",
                headers = headersOf("Content-Type", "application/json"))
        }) { defaultRequest { url("https://fixture.invalid") } }
        val uploader = HttpClient(MockEngine { error("No upload") })
        try {
            val reader = HomeHistoryReaderAdapter(createHomePreparationRepository(client, uploader, "https://fixture.invalid"))
            assertTrue(reader.page(1, 20, null).isFailure)
            assertTrue(reader.calendar(HomeHistoryDateRange("2026-10-16", "2026-10-16")).isFailure)
        } finally { client.close(); uploader.close() }
    }
}
