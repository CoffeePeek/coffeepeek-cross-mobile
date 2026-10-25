package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekStackedCardCarousel
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.feature.profile.domain.PublicProfileFavorite
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun PublicProfileFavoritesPreview(
    favorites: List<PublicProfileFavorite>, image: ProfileImage, logoImage: ProfileImage,
    onOpen: (PublicProfileFavorite) -> Unit,
) {
    CoffeePeekStackedCardCarousel(
        favorites.filter { it.kind == PublicFavoriteKind.CoffeeShop },
        favorites.filter { it.kind == PublicFavoriteKind.Roaster },
        favorites.filter { it.kind == PublicFavoriteKind.Drink }, { it.id },
    ) { favorite, modifier ->
        PublicProfileFavoriteCard(favorite, image, logoImage, modifier) { onOpen(favorite) }
    }
}

@Composable
private fun PublicFavoritesPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicProfileFavoritesPreview(PublicFavoriteKind.entries.map {
        PublicProfileFavorite(it.name, "CoffeePeek", it)
    }, { _, _, _ -> }, { _, _, _ -> }, {})
}

@Preview @Composable private fun PublicFavoritesLightPreview() = PublicFavoritesPreview(false)
@Preview @Composable private fun PublicFavoritesDarkPreview() = PublicFavoritesPreview(true)
