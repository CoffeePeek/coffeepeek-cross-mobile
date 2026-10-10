package com.coffeepeek.feature.favorites.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import com.coffeepeek.feature.favorites.api.FavoritesRoute

/** Root owns back stack, shop routes and NavDisplay decorators; feature registers only its entry. */
fun EntryProviderScope<NavKey>.favoritesEntry(
    screen: FavoritesEntry,
    onOpenShop: (String) -> Unit,
    onBack: () -> Unit,
    distanceForCoordinates: (Double, Double) -> String? = { _, _ -> null },
) {
    entry<FavoritesRoute> { screen.Content(onOpenShop, onBack, distanceForCoordinates) }
}
