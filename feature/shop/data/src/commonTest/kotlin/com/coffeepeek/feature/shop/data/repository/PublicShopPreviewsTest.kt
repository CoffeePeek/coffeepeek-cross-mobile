package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.shop.api.PublicShopCoordinates
import com.coffeepeek.feature.shop.api.PublicShopOpeningHours
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicShopPreviewsTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun publicPreviewKeepsDistinctCardAndDetailVariantsAndDoesNotLoadMenuCatalog() = read(
        """"rating":4.7,"checkInCount":"12","isOpen":true,
            "location":{"latitude":53.9,"longitude":27.5},
            "photos":[{"fullUrl":"full","urls":{"card":"card","detail":"detail"}}],
            "menu":{"items":[{"slug":"coffee"}]},"userCheckIns":[]""",
    ) { reader ->
        val row = reader.load("cafe").getOrThrow()
        assertEquals("card", row.coverPhotoUrl)
        assertEquals("detail", row.detailPhotoUrl)
        assertEquals(4.7, row.rating)
        assertEquals(12, row.reviewCount)
        assertEquals(true, row.isOpen)
        assertEquals(PublicShopCoordinates(53.9, 27.5), row.coordinates)
    }

    @Test fun detailSkipsEmptyPhotosAndBecomesTheCoverWhenTheFirstPhotoIsEmpty() = read(
        """"photos":[{"fullUrl":" "},{"fullUrl":"full","urls":{"detail":"detail"}}]""",
    ) { reader ->
        val row = reader.load("cafe").getOrThrow()
        assertEquals("detail", row.coverPhotoUrl)
        assertEquals("detail", row.detailPhotoUrl)
    }

    @Test fun cardIsTheLastFallbackWhenNoDetailOrFullUrlExists() = read(
        """"photos":[{"urls":{"card":"card"}}]""",
    ) { reader -> assertEquals("card", reader.load("cafe").getOrThrow().detailPhotoUrl) }

    @Test fun emptyPhotosSchedulesAndIncompleteCoordinatesStayAbsent() = read(
        """"rating":0,"location":{"latitude":53.9},"photos":[{"fullUrl":" "}]""",
    ) { reader ->
        val row = reader.load("cafe").getOrThrow()
        assertNull(row.coverPhotoUrl)
        assertNull(row.detailPhotoUrl)
        assertNull(row.rating)
        assertNull(row.coordinates)
        assertNull(row.hours)
    }

    @Test fun hoursUseTheExistingUtcOffsetAndWeekRolloverPolicy() = read(
        """"schedules":[{"dayOfWeek":"Saturday","intervals":[{"openTime":"22:00:00","closeTime":"23:30:00"}]}]""", 180,
    ) { reader -> assertEquals(PublicShopOpeningHours(false, "01:00", "02:30"), reader.load("cafe").getOrThrow().hours) }

    @Test fun closedScheduleHasNoInventedInterval() = read(
        """"schedules":[{"dayOfWeek":1,"isClosed":true}]""",
    ) { reader -> assertEquals(PublicShopOpeningHours(true), reader.load("cafe").getOrThrow().hours) }

    @Test fun failedRequestsStayFailuresAndCancellationEscapes() = runBlocking {
        withReader(MockEngine { respond("""{"isSuccess":false}""", HttpStatusCode.BadRequest, headers) }) { reader ->
            assertTrue(reader.load("cafe").isFailure)
        }
        withReader(MockEngine { throw CancellationException("cancel") }) { reader ->
            assertFailsWith<CancellationException> { reader.load("cafe") }
        }
    }

    private fun read(fields: String, offset: Int = 0, verify: suspend (PublicShopPreviews) -> Unit) = runBlocking {
        var calls = 0
        val engine = MockEngine { request ->
            assertEquals("/api/CoffeeShops/cafe", request.url.encodedPath)
            calls++
            respond("""{"isSuccess":true,"data":{"address":{"slug":"cafe"},$fields}}""", headers = headers)
        }
        withReader(engine, offset, verify)
        assertEquals(1, calls)
    }

    private suspend fun withReader(engine: MockEngine, offset: Int = 0, verify: suspend (PublicShopPreviews) -> Unit) {
        val client = HttpClientFactory(engine).api("https://api.example")
        try { verify(createPublicShopPreviews(client) { offset }) } finally { client.close() }
    }
}
