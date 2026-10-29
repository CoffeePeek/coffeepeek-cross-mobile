package com.coffeepeek.admin.feature.favorites.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekStackedCardCarousel
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Compatibility slot for public-profile cards; geometry lives in the design system. */
@Composable
internal fun <T> FavoritesPreviewCarouselContent(
    coffeeShops: List<T>,
    roasters: List<T>,
    drinks: List<T>,
    itemKey: (T) -> String,
    card: @Composable (T, Modifier) -> Unit,
) = CoffeePeekStackedCardCarousel(coffeeShops, roasters, drinks, itemKey, card = card)

@Composable
private fun CompatibilityCarouselPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FavoritesPreviewCarouselContent(listOf("Кофейня"), listOf("Обжарщик", "Ещё обжарщик"), emptyList(), { it }) { title, modifier ->
        Text(title, modifier)
    }
}

@Preview
@Composable private fun CompatibilityCarouselLightPreview() = CompatibilityCarouselPreview(false)
@Preview
@Composable private fun CompatibilityCarouselDarkPreview() = CompatibilityCarouselPreview(true)
