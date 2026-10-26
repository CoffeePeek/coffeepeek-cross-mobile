package com.coffeepeek.feature.leaderboard.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.impl.ui.LeaderboardViewModel
import com.coffeepeek.feature.leaderboard.impl.ui.compose.component.LeaderboardCard
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardScreen(vm: LeaderboardViewModel, refreshKey: Long, collapsed: Boolean, image: LeaderboardImage,
    onOpenProfile: (String, String, String?) -> Unit,
    overlay: @Composable (content: @Composable () -> Unit) -> Unit, modifier: Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val openProfile by rememberUpdatedState(onOpenProfile)
    var previousRefresh by remember { mutableLongStateOf(refreshKey) }
    LaunchedEffect(refreshKey) {
        if (previousRefresh != refreshKey) vm.onAction(LeaderboardAction.Refresh)
        previousRefresh = refreshKey
    }
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (event.generation == vm.state.value.generation) openProfile(event.slug, event.username, event.avatarUrl)
        }
    }
    LeaderboardScreenContent(state, collapsed, vm::onAction, image, overlay, modifier)
}

@Composable
internal fun LeaderboardScreenContent(state: LeaderboardState, collapsed: Boolean, onAction: (LeaderboardAction) -> Unit,
    image: LeaderboardImage, overlay: @Composable (content: @Composable () -> Unit) -> Unit, modifier: Modifier) {
    LeaderboardCard(state, collapsed, onAction, image, modifier)
    if (state.expanded) overlay { LeaderboardListSheet(state, onAction, image) }
}

@Preview @Composable private fun LeaderboardScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardScreenContent(LeaderboardState(isLoading = false, loadFailed = true), false, {}, { _, _ -> }, { it() }, Modifier)
}

@Preview @Composable private fun LeaderboardScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardScreenContent(LeaderboardState(), false, {}, { _, _ -> }, { it() }, Modifier)
}
