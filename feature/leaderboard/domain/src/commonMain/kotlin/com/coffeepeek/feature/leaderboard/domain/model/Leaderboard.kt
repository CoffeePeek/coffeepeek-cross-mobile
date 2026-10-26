package com.coffeepeek.feature.leaderboard.domain.model

enum class LeaderboardPeriod { Week, Month }

data class LeaderboardEntry(
    val rank: Int,
    val authorId: String,
    val username: String,
    val avatarUrl: String?,
    val authorSlug: String?,
    val points: Int,
    val additions: Int,
    val edits: Int,
    val checkIns: Int,
)

data class LeaderboardPage(
    val period: LeaderboardPeriod,
    val startsOn: String,
    val endsOnExclusive: String,
    val timeZone: String,
    val items: List<LeaderboardEntry>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val me: LeaderboardEntry?,
) {
    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount && page < 10_000
}
