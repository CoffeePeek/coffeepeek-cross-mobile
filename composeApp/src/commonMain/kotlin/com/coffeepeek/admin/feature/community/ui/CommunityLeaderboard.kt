package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.leaderboard.api.LeaderboardEntry
import org.koin.compose.koinInject

@Composable
internal fun CommunityLeaderboard(refreshKey: Long, bottomClearance: Dp, modifier: Modifier) {
    koinInject<LeaderboardEntry>().Content(
        refreshKey = refreshKey,
        bottomClearance = bottomClearance,
        onOpenProfile = { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
        modifier = modifier,
    )
}
