package com.coffeepeek.feature.leaderboard.impl.ui.compose.model

import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod

internal sealed interface LeaderboardAction {
    data object ObserveContext : LeaderboardAction
    data object Refresh : LeaderboardAction
    data class FeedRefreshed(val key: Long) : LeaderboardAction
    data object Retry : LeaderboardAction
    data class SelectPeriod(val period: LeaderboardPeriod) : LeaderboardAction
    data class OpenProfile(val authorId: String) : LeaderboardAction
}
