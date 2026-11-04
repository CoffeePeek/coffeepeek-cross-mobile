package com.coffeepeek.feature.leaderboard.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

typealias LeaderboardImage = @Composable (url: String, modifier: Modifier) -> Unit

/** Full ranking content below the caller's section navigation, with its bottom-bar clearance. */
interface LeaderboardEntry {
    @Composable
    fun Content(
        refreshKey: Long,
        bottomClearance: Dp,
        onOpenProfile: (slug: String, username: String, avatarUrl: String?) -> Unit,
        modifier: Modifier,
    )
}
