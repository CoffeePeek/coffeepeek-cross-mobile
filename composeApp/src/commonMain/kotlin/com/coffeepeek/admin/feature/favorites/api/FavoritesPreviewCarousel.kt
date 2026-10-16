package com.coffeepeek.admin.feature.favorites.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.favorites.ui.FavoritesPreviewCarouselContent

/** Normal café cards followed by pairs of roasters sharing the adjacent café's height. */
@Composable
internal fun <T> FavoritesPreviewCarousel(
    coffeeShops: List<T>,
    roasters: List<T>,
    drinks: List<T>,
    itemKey: (T) -> String,
    card: @Composable (T, Modifier) -> Unit,
) = FavoritesPreviewCarouselContent(coffeeShops, roasters, drinks, itemKey, card)
