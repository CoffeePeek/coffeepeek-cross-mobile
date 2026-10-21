package com.coffeepeek.feature.favorites.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.favorites.api.FavoritesScreenAnalytics

internal val LocalFavoritesAnalytics = staticCompositionLocalOf { FavoritesScreenAnalytics.None }

@Composable
internal fun Modifier.favoriteExposure(roaster: Boolean): Modifier =
    LocalFavoritesAnalytics.current.heart(this, roaster)
