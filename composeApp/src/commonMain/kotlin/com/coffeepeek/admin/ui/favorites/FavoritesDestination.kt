package com.coffeepeek.admin.ui.favorites

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance

/** Android opens the collections library; iOS keeps the saved shops, roasters and drinks lists. */
@Composable
internal expect fun FavoritesDestination()

/** Location permission and formatting belong to application composition. */
internal fun favoriteDistanceLabel(userLocation: GeoPoint?, latitude: Double, longitude: Double): String? =
    formatDistance(distanceToShopMeters(userLocation, latitude, longitude))
