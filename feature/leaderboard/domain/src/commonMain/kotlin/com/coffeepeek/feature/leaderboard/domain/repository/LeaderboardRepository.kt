package com.coffeepeek.feature.leaderboard.domain.repository

import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod

interface LeaderboardRepository {
    /** The top 30 in the server's current calendar period in Europe/Minsk. */
    suspend fun load(period: LeaderboardPeriod): Result<LeaderboardPage>
}
