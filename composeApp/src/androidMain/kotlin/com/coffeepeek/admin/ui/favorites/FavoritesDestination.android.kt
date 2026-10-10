package com.coffeepeek.admin.ui.favorites

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import org.koin.compose.koinInject

@Composable
internal actual fun FavoritesDestination() {
    FavoritesDestination(koinInject<FavoritesEntry>())
}

@Composable
internal fun FavoritesDestination(entry: FavoritesEntry) {
    val userLocation = rememberPermittedUserLocation()
    entry.Content(
        onOpenShop = { shopId -> Navigator.navigate(Navigator.Screen.ShopDetail(shopId)) },
        onBack = Navigator::popBack,
        distanceForCoordinates = { latitude, longitude ->
            favoriteDistanceLabel(userLocation, latitude, longitude)
        },
    )
}

/** Location stays in application composition; the feature only supplies saved coordinates. */
internal fun favoriteDistanceLabel(userLocation: GeoPoint?, latitude: Double, longitude: Double): String? =
    formatDistance(distanceToShopMeters(userLocation, ShopLocation(latitude = latitude, longitude = longitude)))
