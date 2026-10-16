package com.coffeepeek.feature.checkinhistory.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckInHistoryRepositoryTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val period = HistoryPeriod("2026-09-30T21:00:00Z", "2026-10-31T21:00:00Z")

    @Test fun readsMinePaginationAndPreservesOwnerContentAndIssuedUrls() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/check-ins/mine", request.url.encodedPath)
            assertEquals("2", request.url.parameters["pageNumber"])
            assertEquals("20", request.url.parameters["pageSize"])
            assertNull(request.url.parameters["from"])
            respond("""{"isSuccess":true,"data":{"totalCount":"41","items":[{
                "id":"visit","shop":{"slug":"coffee","canonicalPath":"/coffee-shop/coffee"},
                "author":null,"shopName":"Кофейня","username":"Анна","note":"Личная заметка",
                "authorAvatarUrl":"/api/Users/owner/avatar?signature=issued",
                "createdAt":"2026-10-07T10:00:00Z","visitedAt":"2026-10-07T09:00:00Z",
                "rating":{"place":"4","service":5,"coffee":null},
                "visibility":"Private","moderationState":"Rejected","contentRevision":"2",
                "rejectionReason":"Причина","helpfulCount":"7","isHelpfulByCurrentUser":true,
                "drinkSlug":"cappuccino","drinkNameRu":"Капучино",
                "drinkPhotoUrl":"/api/v1/check-ins/visit/drink-photo",
                "photos":[{"url":"https://images.example/last.jpg","sortIndex":2},
                    {"storageKey":"no-guessed-url","sortIndex":0},
                    {"fullUrl":"/api/v1/check-ins/visit/photos/first","sortIndex":"1"}]
            }]}}""", headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://api.example")
        try {
            val result = createCheckInHistoryRepository(client, "https://api.example/").page(2, 20).getOrThrow()
            assertEquals(41, result.totalCount)
            assertEquals(3, result.totalPages)
            assertTrue(result.hasMore)
            val entry = result.items.single()
            assertEquals("https://api.example/api/Users/owner/avatar?signature=issued", entry.authorAvatarUrl)
            assertEquals("coffee", entry.shop?.slug)
            assertEquals("/coffee-shop/coffee", entry.shop?.canonicalPath)
            assertNull(entry.author)
            assertEquals("Личная заметка", entry.note)
            assertEquals("2026-10-07T09:00:00Z", entry.effectiveVisitedAt)
            assertEquals(4, entry.rating?.place)
            assertNull(entry.rating?.coffee)
            assertEquals(HistoryVisibility.Private, entry.visibility)
            assertEquals(HistoryModeration.Rejected, entry.moderation)
            assertEquals("Причина", entry.rejectionReason)
            assertEquals(2, entry.contentRevision)
            assertEquals(7, entry.helpfulCount)
            assertTrue(entry.isHelpfulByCurrentUser)
            assertEquals("cappuccino", entry.drink?.slug)
            assertEquals(listOf("https://api.example/api/v1/check-ins/visit/photos/first", "https://images.example/last.jpg"), entry.photoUrls)
            assertEquals("https://api.example/api/v1/check-ins/visit/drink-photo", entry.drinkPhotoUrl)
        } finally { client.close(); engine.close() }
    }

    @Test fun rangeReadsEveryPageUsingExactCallerBoundsAndDeduplicatesIds() = runBlocking {
        val pages = mutableListOf<Int>()
        val engine = MockEngine { request ->
            assertEquals(period.fromInclusive, request.url.parameters["from"])
            assertEquals(period.toExclusive, request.url.parameters["to"])
            assertEquals("100", request.url.parameters["pageSize"])
            val number = request.url.parameters["pageNumber"]!!.toInt()
            pages += number
            val ids = when (number) {
                1 -> (0..99).toList()
                2 -> (99..198).toList()
                3 -> listOf(199)
                else -> error("Unexpected page")
            }
            val items = ids.joinToString(",") { """{"id":"visit-$it"}""" }
            respond("""{"isSuccess":true,"data":{"totalCount":201,"items":[$items]}}""", headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://api.example")
        try {
            val result = createCheckInHistoryRepository(client, "https://api.example").period(period).getOrThrow()
            assertEquals(listOf(1, 2, 3), pages)
            assertEquals((0..199).map { "visit-$it" }, result.map { it.id })
        } finally { client.close(); engine.close() }
    }

    @Test fun emptyPeriodStopsAfterFirstPageAndNormalizesPagingInputs() = runBlocking {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals("1", request.url.parameters["pageNumber"])
            assertEquals("100", request.url.parameters["pageSize"])
            respond("""{"isSuccess":true,"data":{"totalCount":0,"items":[]}}""", headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://api.example")
        try {
            val repository = createCheckInHistoryRepository(client, "https://api.example")
            assertEquals(emptyList(), repository.period(period).getOrThrow())
            assertEquals(1, calls)
            val page = repository.page(-1, 1000).getOrThrow()
            assertEquals(1, page.page)
            assertEquals(100, page.pageSize)
        } finally { client.close(); engine.close() }
    }

    @Test fun removedRelationsAndUnknownPrivacyNeverInventPublicNavigationOrMedia() = runBlocking {
        val engine = MockEngine {
            respond("""{"isSuccess":true,"data":{"totalCount":1,"items":[{
                "id":"visit","shop":null,"createdAtUtc":"2026-10-01T10:00:00Z",
                "visibility":"FutureValue","moderationState":"FutureValue",
                "drinkPhotoUrl":"javascript:bad","photos":[{"storageKey":"hidden"},{"url":"//foreign.example/photo"}]
            }]}}""", headers = headers)
        }
        val client = HttpClientFactory(engine).api("https://api.example")
        try {
            val item = createCheckInHistoryRepository(client, "https://api.example").page(1, 20).getOrThrow().items.single()
            assertNull(item.shop)
            assertNull(item.rating)
            assertNull(item.drink)
            assertNull(item.drinkPhotoUrl)
            assertTrue(item.photoUrls.isEmpty())
            assertEquals(HistoryVisibility.Private, item.visibility)
            assertEquals(HistoryModeration.Unknown, item.moderation)
            assertEquals("2026-10-01T10:00:00Z", item.effectiveVisitedAt)
        } finally { client.close(); engine.close() }
    }

    @Test fun rejectsHttpEnvelopeMalformedAndInvalidMetadataEvenWithExpectSuccessDisabled() = runBlocking {
        for ((body, status) in listOf(
            """{"isSuccess":true,"data":{"totalCount":0,"items":[]}}""" to HttpStatusCode.Forbidden,
            """{"isSuccess":false,"data":{"totalCount":0,"items":[]}}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":null}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":{"totalCount":-1}}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":{"totalCount":"bad"}}""" to HttpStatusCode.OK,
            """{"isSuccess":true,"data":{"totalCount":1,"items":[{"id":""}]}}""" to HttpStatusCode.OK,
            "not-json" to HttpStatusCode.OK,
        )) {
            val engine = MockEngine { respond(body, status, headers) }
            val client = HttpClientFactory(engine).api("https://api.example") { expectSuccess = false }
            try { assertTrue(createCheckInHistoryRepository(client, "https://api.example").page(1, 20).isFailure) }
            finally { client.close(); engine.close() }
        }
    }

    @Test fun missingOrFailedLaterPageDoesNotPublishAPartialCalendar() = runBlocking {
        for (secondPage in listOf(
            """{"isSuccess":true,"data":{"totalCount":101,"items":[]}}""",
            """{"isSuccess":false,"data":null}""",
        )) {
            val pages = mutableListOf<String?>()
            val engine = MockEngine { request ->
                val page = request.url.parameters["pageNumber"]
                pages += page
                respond(if (page == "1") """{"isSuccess":true,"data":{"totalCount":101,"items":[{"id":"first"}]}}"""
                    else secondPage, headers = headers)
            }
            val client = HttpClientFactory(engine).api("https://api.example")
            try {
                assertTrue(createCheckInHistoryRepository(client, "https://api.example").period(period).isFailure)
                assertEquals(listOf<String?>("1", "2"), pages)
            } finally { client.close(); engine.close() }
        }
    }

    @Test fun cancellationIsNotReturnedAsAResultFailure() = runBlocking<Unit> {
        for (range in listOf(false, true)) {
            var calls = 0
            val engine = MockEngine {
                calls++
                if (range && calls == 1) {
                    respond("""{"isSuccess":true,"data":{"totalCount":101,"items":[{"id":"first"}]}}""", headers = headers)
                } else throw CancellationException("cancelled")
            }
            val client = HttpClientFactory(engine).api("https://api.example")
            try {
                val repository = createCheckInHistoryRepository(client, "https://api.example")
                assertFailsWith<CancellationException> {
                    if (range) repository.period(period) else repository.page(1, 20)
                }
                assertEquals(if (range) 2 else 1, calls)
            } finally { client.close(); engine.close() }
        }
    }
}
