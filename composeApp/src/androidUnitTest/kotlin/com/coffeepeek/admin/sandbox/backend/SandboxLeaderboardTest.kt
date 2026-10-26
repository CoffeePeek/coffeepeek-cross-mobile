package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.leaderboard.data.repository.createLeaderboardRepository
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Test
import java.time.LocalDate
import kotlin.test.*

class SandboxLeaderboardTest {
    private suspend fun fixture(block: suspend (SandboxBackend) -> Unit) {
        val backend = SandboxBackend(byteArrayOf(1), LocalDate.parse("2026-10-26"))
        try { block(backend) } finally { backend.close() }
    }

    @Test fun productionRepositoryReadsTiedRanksAndBothPagesInSandbox() = runTest { fixture { backend ->
        val repository = createLeaderboardRepository(backend.gateway)
        val first = repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow()
        assertEquals(listOf(1, 1, 3), first.items.take(3).map { it.rank })
        assertEquals(20, first.items.size)
        assertEquals(25, first.totalCount)
        assertEquals("2026-10-26", first.startsOn)
        assertNull(first.me)
        val second = repository.load(LeaderboardPeriod.Week, 2, first.startsOn).getOrThrow()
        assertEquals(5, second.items.size)
        assertFalse(second.hasMore)
        assertEquals(SandboxApplicationBackend.SELF_ID, second.items.last().authorId)
        assertEquals(120, repository.load(LeaderboardPeriod.Month, 1, null).getOrThrow().items.first().points)
    } }

    @Test fun authenticatedFixtureIncludesOwnRankOutsideTheFirstPageAndBlocksHideAuthors() = runTest { fixture { backend ->
        val authenticated = backend.gateway.get("/api/v1/leaderboard") { bearerAuth("sandbox-only") }.body<JsonObject>()
        val data = authenticated.getValue("data").jsonObject
        assertEquals(25, data.getValue("me").jsonObject.getValue("rank").jsonPrimitive.int)
        backend.gateway.put("/api/v1/me/blocks/${SandboxApplicationBackend.FRIEND_ID}")
        val page = createLeaderboardRepository(backend.gateway).load(LeaderboardPeriod.Week, 1, null).getOrThrow()
        assertEquals(24, page.totalCount)
        assertTrue(page.items.none { it.authorId == SandboxApplicationBackend.FRIEND_ID })
    } }

    @Test fun emptyPeriodsAndServerErrorsRemainDifferentStates() = runTest { fixture { backend ->
        val repository = createLeaderboardRepository(backend.gateway)
        backend.reset(populated = false)
        val empty = repository.load(LeaderboardPeriod.Week, 1, null).getOrThrow()
        assertTrue(empty.items.isEmpty())
        assertEquals(0, empty.totalCount)
        backend.scenario = SandboxScenario.ServerError
        assertTrue(repository.load(LeaderboardPeriod.Week, 1, null).isFailure)
    } }
}
