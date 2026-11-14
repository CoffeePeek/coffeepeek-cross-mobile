package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.impl.resources.*
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardStatus(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            state.isLoading && state.data == null -> CoffeePeekLoader(contentDescription = stringResource(Res.string.leaderboard_loading))
            state.loadFailed -> {
                Text(stringResource(Res.string.leaderboard_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onAction(LeaderboardAction.Retry) }) {
                    Text(stringResource(Res.string.leaderboard_retry))
                }
            }
            state.data?.items.isNullOrEmpty() -> Text(stringResource(Res.string.leaderboard_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview @Composable private fun LeaderboardStatusLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardStatus(LeaderboardState(isLoading = false, loadFailed = true), {})
}

@Preview @Composable private fun LeaderboardStatusDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardStatus(LeaderboardState(), {})
}
