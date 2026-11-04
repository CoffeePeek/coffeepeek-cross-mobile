package com.coffeepeek.feature.leaderboard.impl.ui.compose.model

internal data class LeaderboardEvent(
    val slug: String,
    val username: String,
    val avatarUrl: String?,
    val generation: Long,
)
