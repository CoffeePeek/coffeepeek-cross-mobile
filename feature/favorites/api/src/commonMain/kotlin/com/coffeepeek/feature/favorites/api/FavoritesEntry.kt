package com.coffeepeek.feature.favorites.api

import androidx.compose.runtime.Composable

/** Minimal screen construction contract; native UI hosts can use domain instead. */
interface FavoritesEntry {
    @Composable
    fun Content(
        onOpenShop: (String) -> Unit,
        onBack: () -> Unit,
        /** Called only for saved shops with both coordinates; the host owns location permission and formatting. */
        distanceForCoordinates: (latitude: Double, longitude: Double) -> String? = { _, _ -> null },
    )
}
