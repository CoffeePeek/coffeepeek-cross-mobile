package com.coffeepeek.feature.leaderboard.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.impl.resources.*
import com.coffeepeek.feature.leaderboard.impl.ui.compose.component.LeaderboardPeriods
import com.coffeepeek.feature.leaderboard.impl.ui.compose.component.LeaderboardRow
import com.coffeepeek.feature.leaderboard.impl.ui.compose.component.LeaderboardStatus
import com.coffeepeek.feature.leaderboard.impl.ui.compose.component.LeaderboardRules
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardListSheet(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit, image: LeaderboardImage) {
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = { onAction(LeaderboardAction.SetExpanded(false)) },
        dismissDescription = stringResource(Res.string.leaderboard_close), maxSheetHeightFraction = 0.9f,
    ) {
        LeaderboardListContent(state, onAction, image)
    }
}

@Composable
internal fun LeaderboardListContent(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit, image: LeaderboardImage) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        item(key = "header") { LeaderboardListHeader(state, onAction) }
        state.data?.me?.let { me -> item(key = "me") {
            CoffeePeekContentCard {
                Text(stringResource(Res.string.leaderboard_own_rank, me.rank), style = MaterialTheme.typography.titleMedium)
                Text(pluralStringResource(Res.plurals.leaderboard_points, me.points, me.points))
            }
        } }
        items(state.data?.items.orEmpty(), key = { "author:${it.authorId}" }) { row ->
            Column {
                LeaderboardRow(row, image, { onAction(LeaderboardAction.OpenProfile(row.authorId)) }, detailed = true)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item(key = "status") { LeaderboardStatus(state, onAction) }
        item(key = "rules") { LeaderboardRules() }
    }
}

@Composable
private fun LeaderboardListHeader(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.leaderboard_title), style = MaterialTheme.typography.titleLarge)
        LeaderboardPeriods(state.period) { onAction(LeaderboardAction.SelectPeriod(it)) }
    }
}

@Composable
private fun LeaderboardListPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val row = LeaderboardEntry(1, "preview", "Maria", null, "maria", 21, 3, 2, 5)
    val data = LeaderboardPage(LeaderboardPeriod.Week, "2026-10-26", "2026-11-02", "Europe/Minsk",
        listOf(row), 1, 1, 30, row)
    Surface { LeaderboardListContent(LeaderboardState(data = data, isLoading = false), {}, { _, _ -> }) }
}

@Preview @Composable private fun LeaderboardListLightPreview() = LeaderboardListPreview(false)
@Preview @Composable private fun LeaderboardListDarkPreview() = LeaderboardListPreview(true)
