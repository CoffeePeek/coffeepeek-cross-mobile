package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_add_roaster
import com.coffeepeek.feature.discovery.impl.resources.discovery_roaster_suggestion_body
import com.coffeepeek.feature.discovery.impl.resources.discovery_roaster_suggestion_title
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_action
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_description
import com.coffeepeek.feature.discovery.impl.resources.discovery_suggestion_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun SearchContributionActions(illustration: Painter, onAddShop: () -> Unit, onAddRoaster: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekCatalogSuggestionCard(
            stringResource(Res.string.discovery_suggestion_title),
            stringResource(Res.string.discovery_suggestion_description),
            stringResource(Res.string.discovery_suggestion_action), illustration, onAddShop,
        )
        if (onAddRoaster != null) CoffeePeekCatalogSuggestionCard(
            stringResource(Res.string.discovery_roaster_suggestion_title),
            stringResource(Res.string.discovery_roaster_suggestion_body),
            stringResource(Res.string.discovery_add_roaster), illustration, onAddRoaster,
        )
    }
}

@Preview
@Composable
private fun SearchContributionActionsPreview() = CoffeePeekTheme {
    Surface { SearchContributionActions(ColorPainter(Color.LightGray), {}, {}) }
}
