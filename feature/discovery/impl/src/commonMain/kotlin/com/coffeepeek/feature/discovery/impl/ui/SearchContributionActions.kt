package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R

@Composable
internal fun SearchContributionActions(illustration: Painter, onAddShop: () -> Unit, onAddRoaster: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekCatalogSuggestionCard(
            stringResource(R.string.discovery_suggestion_title),
            stringResource(R.string.discovery_suggestion_description),
            stringResource(R.string.discovery_suggestion_action), illustration, onAddShop,
        )
        if (onAddRoaster != null) CoffeePeekCatalogSuggestionCard(
            stringResource(R.string.discovery_roaster_suggestion_title),
            stringResource(R.string.discovery_roaster_suggestion_body),
            stringResource(R.string.discovery_add_roaster), illustration, onAddRoaster,
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchContributionActionsPreview() = CoffeePeekTheme {
    Surface { SearchContributionActions(ColorPainter(Color.LightGray), {}, {}) }
}
