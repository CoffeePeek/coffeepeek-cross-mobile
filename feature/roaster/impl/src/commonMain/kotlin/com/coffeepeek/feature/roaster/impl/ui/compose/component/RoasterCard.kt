package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekRoasterCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_logo
import com.coffeepeek.feature.roaster.impl.resources.roaster_stat_catalog
import com.coffeepeek.feature.roaster.impl.resources.roaster_stat_shops
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Catalogue card: the design-system card with this feature's texts, logo loading and favorite heart. */
@Composable
internal fun RoasterCard(
    name: String,
    photoUrl: String?,
    description: String?,
    shopsCount: Int,
    productsCount: Int,
    isFavorite: Boolean,
    onOpen: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    image: RoasterImage,
    modifier: Modifier = Modifier,
    heartModifier: Modifier = Modifier,
) {
    val photo = photoUrl?.takeIf(String::isNotBlank)
    val logoDescription = stringResource(Res.string.roaster_logo, name)
    CoffeePeekRoasterCard(
        name = name,
        description = description,
        shopsCount = shopsCount,
        shopsLabel = stringResource(Res.string.roaster_stat_shops),
        productsCount = productsCount,
        productsLabel = stringResource(Res.string.roaster_stat_catalog),
        modifier = modifier,
        hasLogo = photo != null,
        onClick = onOpen,
        logo = { photo?.let { image(it, logoDescription, Modifier.fillMaxSize(), ContentScale.Fit, 6.sp) } },
        favorite = onToggleFavorite?.let { toggle -> { RoasterFavoriteHeart(isFavorite, toggle, heartModifier) } },
    )
}

@Preview @Composable private fun RoasterCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterCard("Sandbox Roaster", null, "Локальный обжарщик для тестирования", 7, 21, isFavorite = true,
        onOpen = {}, onToggleFavorite = {}, image = { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterCard("Sandbox Roaster", null, "Локальный обжарщик для тестирования", 7, 21, isFavorite = true,
        onOpen = {}, onToggleFavorite = {}, image = { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterCardEmptyStatsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterCard("Очень длинное название обжарщика для проверки обрезки строки", "https://x", null, 0, 0, isFavorite = false,
        onOpen = null, onToggleFavorite = null, image = { _, _, _, _, _ -> })
}

@Preview @Composable private fun RoasterCardEmptyStatsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterCard("Очень длинное название обжарщика для проверки обрезки строки", "https://x", null, 0, 0, isFavorite = false,
        onOpen = null, onToggleFavorite = null, image = { _, _, _, _, _ -> })
}
