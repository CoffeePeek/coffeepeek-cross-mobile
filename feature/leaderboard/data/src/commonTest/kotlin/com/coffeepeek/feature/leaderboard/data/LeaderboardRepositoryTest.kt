package com.coffeepeek.feature.leaderboard.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.leaderboard.data.repository.createLeaderboardRepository
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private const val entry = """{"rank":1,"authorId":"author-id","username":"Maria","avatarUrl":"https://media.example/avatar",
    "address":{"slug":"maria","canonicalPath":"/users/maria"},"points":21,"additions":3,"edits":2,"checkIns":5}"""

private fun response(page: Int = 1, period: String = "week") = """{"isSuccess":true,"data":{
    "period":"$period","startsOn":"2026-10-26","endsOnExclusive":"2026-11-02","timeZone":"Europe/Minsk",
    "items":[$entry],"totalCount":21,"page":$page,"pageSize":20,"me":$entry}}"""

class LeaderboardRepositoryTest {
    private suspend fun withRepository(engine: MockEngine, block: suspend (LeaderboardRepository) -> Unit) {
        val client = HttpClientFactory(engine).api("https://api.example")
        try { block(createLeaderboardRepository(client)) } finally { client.close() }
    }

    @Test fun usesPublicGetWithPeriodPagingAndNoStore() = runTest {
        withRepository(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/leaderboard", request.url.encodedPath)
            assertEquals("month", request.url.parameters["period"])
            assertEquals("1", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["pageSize"])
            assertNull(request.url.parameters["date"])
            assertNull(request.url.parameters["authorId"])
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            respond(response(period = "month"), headers = jsonHeaders)
        }) { repository -> assertTrue(repository.load(LeaderboardPeriod.Month, 1, null).isSuccess) }
    }

    @Test fun mapsOwnRankAddressAndActionCountsWithoutRecalculatingPoints() = runTest {
        withRepository(MockEngine { respond(response(), headers = jsonHeaders) }) { repository ->
            val page = repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow()
            assertEquals("Europe/Minsk", page.timeZone)
            assertTrue(page.hasMore)
            assertEquals(page.items.single(), page.me)
            val row = page.me!!
            assertEquals("maria", row.authorSlug)
            assertEquals("https://media.example/avatar", row.avatarUrl)
            assertEquals(listOf(21, 3, 2, 5), listOf(row.points, row.additions, row.edits, row.checkIns))
        }
    }

    @Test fun pinsAdditionalPagesToTheFirstCalendarPeriod() = runTest {
        withRepository(MockEngine { request ->
            assertEquals("2026-10-26", request.url.parameters["date"])
            assertEquals("2", request.url.parameters["page"])
            respond(response(page = 2), headers = jsonHeaders)
        }) { repository -> assertFalse(repository.load(LeaderboardPeriod.Week, 2, "2026-10-26").getOrThrow().hasMore) }
    }

    @Test fun preservesCompetitionRanksAndDoesNotInventPublicSlugs() = runTest {
        val rows = listOf(entry, entry.replace("author-id", "b").replace("\"maria\"", "\"\""),
            entry.replace("author-id", "c").replace("\"rank\":1", "\"rank\":3"))
        val payload = response().replace("\"items\":[$entry]", "\"items\":[${rows.joinToString(",")}]")
        withRepository(MockEngine { respond(payload, headers = jsonHeaders) }) { repository ->
            val page = repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow()
            assertEquals(listOf(1, 1, 3), page.items.map { it.rank })
            assertNull(page.items[1].authorSlug)
        }
    }

    @Test fun emptyAnonymousPeriodIsSuccessWithNoOwnRow() = runTest {
        val payload = response().replace("[$entry]", "[]").replace("\"me\":$entry", "\"me\":null")
            .replace("\"totalCount\":21", "\"totalCount\":0")
        withRepository(MockEngine { respond(payload, headers = jsonHeaders) }) { repository ->
            val page = repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow()
            assertTrue(page.items.isEmpty())
            assertFalse(page.hasMore)
            assertNull(page.me)
        }
    }

    @Test fun invalidEnvelopeMissingDataBadCountersAndMismatchedPagesAreFailures() = runTest {
        val invalid = listOf("{\"isSuccess\":false}", "{\"isSuccess\":true,\"data\":null}",
            response().replace("\"points\":21", "\"points\":-1"), response(page = 2), response(period = "future"))
        for (payload in invalid) withRepository(MockEngine { respond(payload, headers = jsonHeaders) }) { repository ->
            assertTrue(repository.load(LeaderboardPeriod.Week, 1, null).isFailure)
        }
    }

    @Test fun invalidPageDoesNotSendRequestAndServerFailuresAreResults() = runTest {
        var requests = 0
        withRepository(MockEngine { requests++; respond("unavailable", HttpStatusCode.ServiceUnavailable) }) { repository ->
            assertTrue(repository.load(LeaderboardPeriod.Week, 0, null).isFailure)
            assertEquals(0, requests)
            assertTrue(repository.load(LeaderboardPeriod.Week, 1, null).isFailure)
            assertEquals(1, requests)
        }
    }

    @Test fun everyReadUsesTheTransportAndCancellationEscapesResult() = runTest {
        var requests = 0
        withRepository(MockEngine { requests++; respond(response(), headers = jsonHeaders) }) { repository ->
            repeat(2) { repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow() }
            assertEquals(2, requests)
        }
        withRepository(MockEngine { throw CancellationException("cancelled") }) { repository ->
            assertFailsWith<CancellationException> { repository.load(LeaderboardPeriod.Week, 1, null) }
        }
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
