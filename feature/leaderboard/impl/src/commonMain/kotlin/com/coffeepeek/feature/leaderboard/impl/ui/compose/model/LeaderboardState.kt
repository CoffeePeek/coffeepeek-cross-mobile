package com.coffeepeek.feature.leaderboard.impl.ui.compose.model

import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod

internal data class LeaderboardState(
    val period: LeaderboardPeriod = LeaderboardPeriod.Week,
    val data: LeaderboardPage? = null,
    val isLoading: Boolean = true,
    val failedPage: Int? = null,
    val expanded: Boolean = false,
    val generation: Long = 0,
)
