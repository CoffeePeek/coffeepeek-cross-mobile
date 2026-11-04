package com.coffeepeek.feature.leaderboard.data.backend

import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.model.LEADERBOARD_LIMIT
import com.coffeepeek.core.network.envelopeData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

internal class LeaderboardBackend(private val client: HttpClient) {
    suspend fun load(period: LeaderboardPeriod): LeaderboardPageDto {
        val response = client.get("/api/v1/leaderboard") {
            parameter("period", if (period == LeaderboardPeriod.Week) "week" else "month")
            parameter("page", 1)
            parameter("pageSize", LEADERBOARD_LIMIT)
            header(HttpHeaders.CacheControl, "no-store")
        }
        check(response.status.isSuccess()) { "Leaderboard request failed" }
        return response.envelopeData<LeaderboardPageDto> { IllegalStateException("Leaderboard request failed") }
    }
}
