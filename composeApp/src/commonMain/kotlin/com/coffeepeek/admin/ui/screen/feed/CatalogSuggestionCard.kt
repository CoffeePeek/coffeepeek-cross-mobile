package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.catalog_shop_suggestion_title
import coffeepeek.composeapp.generated.resources.catalog_shop_suggestion_description
import coffeepeek.composeapp.generated.resources.catalog_shop_suggestion_action
import coffeepeek.composeapp.generated.resources.catalog_roaster_suggestion_title
import coffeepeek.composeapp.generated.resources.catalog_roaster_suggestion_description
import coffeepeek.composeapp.generated.resources.catalog_roaster_suggestion_action
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import com.coffeepeek.core.designsystem.component.CoffeePeekCatalogSuggestionCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal enum class CatalogSuggestionKind { Shop, Roaster }

/** Discovery owns the branded illustration, copy and entry into the existing creation flows. */
@Composable
internal fun CatalogSuggestionCard(kind: CatalogSuggestionKind, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val shop = kind == CatalogSuggestionKind.Shop
    CoffeePeekCatalogSuggestionCard(
        title = stringResource(if (shop) Res.string.catalog_shop_suggestion_title else Res.string.catalog_roaster_suggestion_title),
        description = stringResource(if (shop) Res.string.catalog_shop_suggestion_description else Res.string.catalog_roaster_suggestion_description),
        actionLabel = stringResource(if (shop) Res.string.catalog_shop_suggestion_action else Res.string.catalog_roaster_suggestion_action),
        illustration = painterResource(Res.drawable.maskot_with_magnifying_glass), onAdd = onAdd, modifier = modifier,
    )
}

@Composable
private fun CatalogContributionPreview(dark: Boolean, kind: CatalogSuggestionKind) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CatalogSuggestionCard(kind, {}, Modifier.padding(16.dp)) }
}

@Preview @Composable private fun ShopContributionLightPreview() = CatalogContributionPreview(false, CatalogSuggestionKind.Shop)
@Preview @Composable private fun ShopContributionDarkPreview() = CatalogContributionPreview(true, CatalogSuggestionKind.Shop)
@Preview @Composable private fun RoasterContributionLightPreview() = CatalogContributionPreview(false, CatalogSuggestionKind.Roaster)
@Preview @Composable private fun RoasterContributionDarkPreview() = CatalogContributionPreview(true, CatalogSuggestionKind.Roaster)
