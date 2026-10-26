package com.coffeepeek.feature.leaderboard.data.backend

import kotlinx.serialization.Serializable

@Serializable
internal data class LeaderboardPageDto(
    val period: String,
    val startsOn: String,
    val endsOnExclusive: String,
    val timeZone: String,
    val items: List<LeaderboardEntryDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val me: LeaderboardEntryDto? = null,
)

@Serializable
internal data class LeaderboardEntryDto(
    val rank: Int,
    val authorId: String,
    val username: String,
    val avatarUrl: String? = null,
    val address: LeaderboardAddressDto? = null,
    val points: Int,
    val additions: Int,
    val edits: Int,
    val checkIns: Int,
)

@Serializable
internal data class LeaderboardAddressDto(val slug: String? = null)
