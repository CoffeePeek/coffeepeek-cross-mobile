package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.impl.resources.*
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardCard(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit,
    image: LeaderboardImage, modifier: Modifier = Modifier) {
    CoffeePeekContentCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.leaderboard_title), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { onAction(LeaderboardAction.SetExpanded(true)) }) {
                Text(stringResource(Res.string.leaderboard_all))
                Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(16.dp))
            }
        }
        LeaderboardPeriods(state.period) { onAction(LeaderboardAction.SelectPeriod(it)) }
        state.data?.items?.take(3)?.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LeaderboardRow(row, image, { onAction(LeaderboardAction.OpenProfile(row.authorId)) }, detailed = false)
        }
        if (state.isLoading || state.failedPage != null || state.data?.items.isNullOrEmpty()) {
            LeaderboardStatus(state, onAction)
        }
    }
}

@Composable
private fun LeaderboardCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val rows = listOf(
        LeaderboardEntry(1, "a", "Maria", null, "maria", 21, 3, 2, 5),
        LeaderboardEntry(1, "b", "Имя участника", null, null, 21, 1, 2, 8),
        LeaderboardEntry(3, "c", "Очень длинное имя участника", null, null, 12, 2, 0, 3),
    )
    val data = LeaderboardPage(LeaderboardPeriod.Week, "2026-10-26", "2026-11-02", "Europe/Minsk", rows, 3, 1, 20, null)
    LeaderboardCard(LeaderboardState(data = data, isLoading = false), {}, { _, _ -> })
}

@Preview @Composable private fun LeaderboardCardLightPreview() = LeaderboardCardPreview(false)
@Preview @Composable private fun LeaderboardCardDarkPreview() = LeaderboardCardPreview(true)
