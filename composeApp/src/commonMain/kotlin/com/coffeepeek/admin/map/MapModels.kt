package com.coffeepeek.admin.map

/** What the application's map canvas (MapLibre on Android, the native map on iOS) draws; Discovery supplies it. */
data class MapBounds(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
)

data class MapShop(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = MapShopType.COFFEE_BAR,
    val primaryZoneId: String? = null,
)

data class MapCluster(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val bounds: MapBounds,
)

data class MapCoffeeZone(
    val id: String,
    val name: String,
    val description: String,
    val color: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val shopCount: Int,
    // (latitude, longitude); empty → draw a circle from radiusMeters
    val polygon: List<Pair<Double, Double>> = emptyList(),
)

/** The pin styles the map knows. */
object MapShopType {
    const val SPECIALTY = "specialty"
    const val COFFEE_BAR = "coffee_bar"
    const val CAFE = "cafe"
}
