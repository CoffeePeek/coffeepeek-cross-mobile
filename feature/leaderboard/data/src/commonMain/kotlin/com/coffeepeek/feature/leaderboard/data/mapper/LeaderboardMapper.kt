package com.coffeepeek.feature.leaderboard.data.mapper

import com.coffeepeek.feature.leaderboard.data.backend.LeaderboardEntryDto
import com.coffeepeek.feature.leaderboard.data.backend.LeaderboardPageDto
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod

internal fun LeaderboardPageDto.toDomain(): LeaderboardPage {
    require(page in 1..10_000 && pageSize in 1..100 && totalCount >= 0)
    val kind = when (period) {
        "week" -> LeaderboardPeriod.Week
        "month" -> LeaderboardPeriod.Month
        else -> error("Unknown leaderboard period")
    }
    return LeaderboardPage(kind, startsOn, endsOnExclusive, timeZone, items.map { it.toDomain() },
        totalCount, page, pageSize, me?.toDomain())
}

private fun LeaderboardEntryDto.toDomain(): LeaderboardEntry {
    require(rank > 0 && authorId.isNotBlank() && points >= 0 && additions >= 0 && edits >= 0 && checkIns >= 0)
    return LeaderboardEntry(rank, authorId, username, avatarUrl?.takeIf(String::isNotBlank),
        address?.slug?.takeIf(String::isNotBlank), points, additions, edits, checkIns)
}
