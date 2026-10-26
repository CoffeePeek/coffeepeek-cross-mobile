package com.coffeepeek.feature.leaderboard.domain.repository

import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod

interface LeaderboardRepository {
    /** Null date selects the server's current calendar period in Europe/Minsk. */
    suspend fun load(period: LeaderboardPeriod, page: Int, date: String?): Result<LeaderboardPage>
}
