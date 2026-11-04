package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardRules() {
    Text(stringResource(Res.string.leaderboard_rules_summary), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview @Composable private fun LeaderboardRulesLightPreview() = CoffeePeekTheme(darkTheme = false) { LeaderboardRules() }
@Preview @Composable private fun LeaderboardRulesDarkPreview() = CoffeePeekTheme(darkTheme = true) { LeaderboardRules() }
