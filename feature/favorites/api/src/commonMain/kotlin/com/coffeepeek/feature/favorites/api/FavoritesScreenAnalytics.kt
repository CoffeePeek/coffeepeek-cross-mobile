package com.coffeepeek.feature.favorites.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Visibility metadata only; application composition owns the consent-aware observer. */
interface FavoritesScreenAnalytics {
    @Composable fun heart(modifier: Modifier, roaster: Boolean): Modifier = modifier
    companion object { val None: FavoritesScreenAnalytics = object : FavoritesScreenAnalytics {} }
}
