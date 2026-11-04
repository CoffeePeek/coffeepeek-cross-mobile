package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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
internal fun LeaderboardCard(state: LeaderboardState, collapsed: Boolean, onAction: (LeaderboardAction) -> Unit,
    image: LeaderboardImage, modifier: Modifier = Modifier) {
    CoffeePeekContentCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.leaderboard_title), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { onAction(LeaderboardAction.SetExpanded(true)) }) {
                Text(stringResource(Res.string.leaderboard_all))
                if (state.isLoading && state.data != null) {
                    CoffeePeekLoader(stringResource(Res.string.leaderboard_loading), size = 16.dp)
                } else Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(16.dp))
            }
        }
        AnimatedVisibility(visible = !collapsed, enter = expandVertically(tween(180)), exit = shrinkVertically(tween(180))) {
            Column {
                LeaderboardPeriods(state.period) { onAction(LeaderboardAction.SelectPeriod(it)) }
                LeaderboardPreview(state, onAction, image)
            }
        }
    }
}

@Composable
private fun LeaderboardPreview(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit, image: LeaderboardImage) {
    val rows = state.data?.items.orEmpty()
    Box(Modifier.fillMaxWidth().heightIn(min = leaderboardPreviewHeight()), contentAlignment = Alignment.Center) {
        if (state.loadFailed || rows.isEmpty()) {
            LeaderboardStatus(state, onAction)
        } else Column(Modifier.fillMaxWidth().align(Alignment.TopStart)) {
            rows.take(3).forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LeaderboardRow(row, image, { onAction(LeaderboardAction.OpenProfile(row.authorId)) }, detailed = false)
            }
        }
    }
}

@Composable
private fun leaderboardPreviewHeight(): Dp {
    val typography = MaterialTheme.typography
    val textHeight = with(LocalDensity.current) {
        typography.bodyLarge.lineHeight.toDp() +
            if (fontScale > 1.3f) typography.bodyMedium.lineHeight.toDp() else 0.dp
    }
    return (maxOf(32.dp, textHeight) + CoffeePeekDimens.spacing2 * 2) * 3 + 2.dp
}

@Composable
private fun LeaderboardCardPreview(dark: Boolean, refreshing: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val rows = listOf(
        LeaderboardEntry(1, "a", "Maria", null, "maria", 21, 3, 2, 5),
        LeaderboardEntry(1, "b", "Имя участника", null, null, 21, 1, 2, 8),
        LeaderboardEntry(3, "c", "Очень длинное имя участника", null, null, 12, 2, 0, 3),
    )
    val data = LeaderboardPage(LeaderboardPeriod.Week, "2026-10-26", "2026-11-02", "Europe/Minsk", rows, 3, 1, 30, null)
    LeaderboardCard(LeaderboardState(period = if (refreshing) LeaderboardPeriod.Month else LeaderboardPeriod.Week,
        data = data, isLoading = refreshing), false, {}, { _, _ -> })
}

@Preview @Composable private fun LeaderboardCardLightPreview() = LeaderboardCardPreview(false)
@Preview @Composable private fun LeaderboardCardDarkPreview() = LeaderboardCardPreview(true)
@Preview @Composable private fun LeaderboardRefreshingLightPreview() = LeaderboardCardPreview(false, true)
@Preview @Composable private fun LeaderboardRefreshingDarkPreview() = LeaderboardCardPreview(true, true)

@Preview @Composable private fun LeaderboardCollapsedLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardCard(LeaderboardState(), true, {}, { _, _ -> })
}
@Preview @Composable private fun LeaderboardCollapsedDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardCard(LeaderboardState(), true, {}, { _, _ -> })
}

@Preview @Composable private fun LeaderboardLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardCard(LeaderboardState(), false, {}, { _, _ -> })
}
@Preview @Composable private fun LeaderboardLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardCard(LeaderboardState(), false, {}, { _, _ -> })
}
