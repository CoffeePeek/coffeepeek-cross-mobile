package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardPeriods(selected: LeaderboardPeriod, onSelected: (LeaderboardPeriod) -> Unit) {
    val week = stringResource(Res.string.leaderboard_week)
    val month = stringResource(Res.string.leaderboard_month)
    CoffeePeekCapsuleSegmentedControl(
        options = LeaderboardPeriod.entries, selected = selected,
        label = { if (it == LeaderboardPeriod.Week) week else month }, onSelected = onSelected,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

@Preview @Composable private fun LeaderboardPeriodsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LeaderboardPeriods(LeaderboardPeriod.Week, {})
}

@Preview @Composable private fun LeaderboardPeriodsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LeaderboardPeriods(LeaderboardPeriod.Month, {})
}
