package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RoasterCatalogSuggestion(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekCatalogSuggestionCard(stringResource(Res.string.roaster_suggestion_title),
        stringResource(Res.string.roaster_suggestion_description), stringResource(Res.string.roaster_suggestion_action),
        painterResource(CoffeePeekIllustrations.CatalogSearch), onAdd, modifier)
}

@Preview @Composable private fun RoasterCatalogSuggestionLightPreview() = CoffeePeekTheme(darkTheme = false) { RoasterCatalogSuggestion({}) }
@Preview @Composable private fun RoasterCatalogSuggestionDarkPreview() = CoffeePeekTheme(darkTheme = true) { RoasterCatalogSuggestion({}) }
