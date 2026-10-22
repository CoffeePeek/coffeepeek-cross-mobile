package com.coffeepeek.feature.discovery.domain.model

data class MapBounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

/** A shop on the map; the map needs no more than where it is, its name and its kind. */
data class MapMarker(val id: String, val title: String, val coordinates: Coordinates, val kind: ShopKind = ShopKind.CoffeeBar, val zoneId: String? = null)

/** A coffee zone: a named area with its shop count, drawn as [polygon] or, without one, as a circle of [radiusMeters]. */
data class MapZone(
    val id: String,
    val name: String,
    val description: String,
    val color: String?,
    val center: Coordinates,
    val radiusMeters: Double,
    val shopCount: Int,
    val polygon: List<Coordinates> = emptyList(),
)

/** The server also sends clusters; the legacy map never used them and neither does this model. */
data class MapContent(val markers: List<MapMarker> = emptyList(), val zones: List<MapZone> = emptyList(), val isTruncated: Boolean = false)
