package com.coffeepeek.feature.leaderboard.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.leaderboard.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LeaderboardRules() {
    CoffeePeekContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.leaderboard_rules_title), style = MaterialTheme.typography.titleMedium)
            listOf(Res.string.leaderboard_rules_ranking, Res.string.leaderboard_rules_counts,
                Res.string.leaderboard_rules_points, Res.string.leaderboard_rules_eligibility,
                Res.string.leaderboard_rules_periods).forEach { resource ->
                Text(stringResource(resource), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview @Composable private fun LeaderboardRulesLightPreview() = CoffeePeekTheme(darkTheme = false) { LeaderboardRules() }
@Preview @Composable private fun LeaderboardRulesDarkPreview() = CoffeePeekTheme(darkTheme = true) { LeaderboardRules() }
