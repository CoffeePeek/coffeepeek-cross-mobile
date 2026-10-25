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

/** Both navigation hosts use the same feature entry; platform navigation stays with the app. */
@Composable
internal fun FavoritesDestination() = FavoritesDestination(koinInject<FavoritesEntry>())

@Composable
internal fun FavoritesDestination(entry: FavoritesEntry) {
    val userLocation = rememberPermittedUserLocation()
    entry.Content(
        onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
        onBack = Navigator::popBack,
        onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
        distanceForCoordinates = { latitude, longitude -> favoriteDistanceLabel(userLocation, latitude, longitude) },
    )
}

/** A distinct feature ViewModel is owned by the Profile route, not the application singleton. */
@Composable
internal fun FavoritesProfilePreview(
    onOpenShop: (String) -> Unit,
    onOpenRoaster: (String) -> Unit,
    header: @Composable () -> Unit,
) {
    val userLocation = rememberPermittedUserLocation()
    koinInject<FavoritesEntry>().Preview(onOpenShop, onOpenRoaster,
        { latitude, longitude -> favoriteDistanceLabel(userLocation, latitude, longitude) }, header)
}

/** Location permission and formatting belong to application composition. */
internal fun favoriteDistanceLabel(userLocation: GeoPoint?, latitude: Double, longitude: Double): String? =
    formatDistance(distanceToShopMeters(userLocation, ShopLocation(latitude = latitude, longitude = longitude)))
