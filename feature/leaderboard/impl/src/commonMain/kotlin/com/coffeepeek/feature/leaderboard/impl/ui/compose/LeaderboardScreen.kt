package com.coffeepeek.feature.leaderboard.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.impl.ui.LeaderboardViewModel
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardScreen(vm: LeaderboardViewModel, refreshKey: Long, bottomClearance: Dp, image: LeaderboardImage,
    onOpenProfile: (String, String, String?) -> Unit,
    modifier: Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val openProfile by rememberUpdatedState(onOpenProfile)
    LaunchedEffect(vm, refreshKey) { vm.onAction(LeaderboardAction.FeedRefreshed(refreshKey)) }
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event.generation == vm.state.value.generation) openProfile(event.slug, event.username, event.avatarUrl)
        }
    }
    LeaderboardScreenContent(state, vm::onAction, image, modifier, bottomClearance)
}

@Composable
internal fun LeaderboardScreenContent(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit,
    image: LeaderboardImage, modifier: Modifier = Modifier, bottomClearance: Dp = 0.dp) {
    LeaderboardListContent(state, onAction, image, modifier, bottomClearance)
}

@Preview @Composable private fun LeaderboardScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardScreenContent(LeaderboardState(isLoading = false, loadFailed = true), {}, { _, _ -> })
}
@Preview @Composable private fun LeaderboardScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardScreenContent(LeaderboardState(), {}, { _, _ -> })
}
