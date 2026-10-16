package com.coffeepeek.admin.feature.favorites.api

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.favorites.ui.FavoritesProfilePreviewContent

/** Current-account favorites preview; account state and mutations stay inside favorites. */
@Composable
internal fun FavoritesProfilePreview(
    onCoffeeShopClick: (String) -> Unit,
    onRoasterClick: (String) -> Unit,
) = FavoritesProfilePreviewContent(onCoffeeShopClick, onRoasterClick)
