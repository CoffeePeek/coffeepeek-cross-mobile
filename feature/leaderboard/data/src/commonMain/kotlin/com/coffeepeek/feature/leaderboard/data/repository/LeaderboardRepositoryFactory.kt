package com.coffeepeek.feature.leaderboard.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.leaderboard.data.backend.LeaderboardBackend
import com.coffeepeek.feature.leaderboard.data.mapper.toDomain
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.model.LEADERBOARD_LIMIT
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import io.ktor.client.HttpClient

/** The application owns the configured client. Personalized results are never persisted. */
fun createLeaderboardRepository(client: HttpClient): LeaderboardRepository =
    RemoteLeaderboardRepository(LeaderboardBackend(client))

private class RemoteLeaderboardRepository(private val backend: LeaderboardBackend) : LeaderboardRepository {
    override suspend fun load(period: LeaderboardPeriod) = requestResult {
        backend.load(period).toDomain().also {
            check(it.period == period && it.page == 1 && it.pageSize == LEADERBOARD_LIMIT) { "Unexpected leaderboard page" }
        }
    }
}
