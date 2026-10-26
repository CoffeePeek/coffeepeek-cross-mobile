package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.leaderboard.api.LeaderboardEntry
import org.koin.compose.koinInject

@Composable
internal fun CommunityLeaderboard(refreshKey: Long) {
    koinInject<LeaderboardEntry>().Content(
        refreshKey = refreshKey,
        onOpenProfile = { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
        overlay = { content -> AnalyticsOverlay { content() } }, modifier = Modifier,
    )
}
