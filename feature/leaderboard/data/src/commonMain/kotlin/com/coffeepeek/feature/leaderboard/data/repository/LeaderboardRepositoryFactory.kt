package com.coffeepeek.feature.leaderboard.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.leaderboard.data.backend.LeaderboardBackend
import com.coffeepeek.feature.leaderboard.data.mapper.toDomain
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import io.ktor.client.HttpClient

/** The application owns the configured client. Personalized results are never persisted. */
fun createLeaderboardRepository(client: HttpClient): LeaderboardRepository =
    RemoteLeaderboardRepository(LeaderboardBackend(client))

private class RemoteLeaderboardRepository(private val backend: LeaderboardBackend) : LeaderboardRepository {
    override suspend fun load(period: LeaderboardPeriod, page: Int, date: String?) = requestResult {
        backend.load(period, page, date).toDomain().also {
            check(it.period == period && it.page == page) { "Unexpected leaderboard page" }
        }
    }
}
