package com.coffeepeek.feature.leaderboard.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias LeaderboardImage = @Composable (url: String, modifier: Modifier) -> Unit

interface LeaderboardEntry {
    @Composable
    fun Content(
        refreshKey: Long,
        onOpenProfile: (slug: String, username: String, avatarUrl: String?) -> Unit,
        overlay: @Composable (content: @Composable () -> Unit) -> Unit,
        modifier: Modifier,
    )
}
