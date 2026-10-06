package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.location.GeoPoint
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ShopLocation
import kotlin.math.PI
import kotlin.math.cos

internal const val MAP_NEARBY_RADIUS_METERS = 5_000.0

internal fun nearestMapShops(shops: List<MapShop>, origin: GeoPoint): List<MapShop> =
    shops.distinctBy { it.id }.mapNotNull { shop ->
        val distance = distanceToShopMeters(origin, ShopLocation("", shop.latitude, shop.longitude))
            ?: return@mapNotNull null
        if (distance <= MAP_NEARBY_RADIUS_METERS) shop to distance else null
    }.sortedWith(compareBy<Pair<MapShop, Double>> { it.second }.thenBy { it.first.id })
        .take(10).map { it.first }

internal fun nearbyMapBounds(origin: GeoPoint): MapBounds {
    val latitudeMargin = MAP_NEARBY_RADIUS_METERS / 110_000.0
    val longitudeMargin = latitudeMargin / cos((kotlin.math.abs(origin.latitude) + latitudeMargin) * PI / 180).coerceAtLeast(0.01)
    // ponytail: city maps don't cross the antimeridian; split bounds if global coverage is needed.
    return MapBounds(
        (origin.latitude - latitudeMargin).coerceAtLeast(-85.0),
        (origin.longitude - longitudeMargin).coerceAtLeast(-180.0),
        (origin.latitude + latitudeMargin).coerceAtMost(85.0),
        (origin.longitude + longitudeMargin).coerceAtMost(180.0),
    )
}

internal fun carouselStartPage(count: Int, selectedIndex: Int): Int {
    if (count <= 1) return 0
    val middle = Int.MAX_VALUE / 2
    return middle - middle % count + selectedIndex.coerceIn(0, count - 1)
}
