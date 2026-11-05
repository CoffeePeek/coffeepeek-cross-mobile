package com.coffeepeek.admin.ui.favorites

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import org.koin.compose.koinInject

@Composable
internal actual fun FavoritesDestination() {
    val userLocation = rememberPermittedUserLocation()
    koinInject<FavoritesEntry>().Content(
        onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
        onBack = Navigator::popBack,
        onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
        onShare = { Navigator.navigate(Navigator.Screen.CollectionsLibrary) },
        onOpenCollections = { Navigator.navigate(Navigator.Screen.CollectionsLibrary) },
        distanceForCoordinates = { latitude, longitude -> favoriteDistanceLabel(userLocation, latitude, longitude) },
    )
}
