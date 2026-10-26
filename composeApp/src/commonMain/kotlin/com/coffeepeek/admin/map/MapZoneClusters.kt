package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone

internal fun zoneMapCluster(zone: MapCoffeeZone): MapCluster {
    val points = mapZoneOutline(zone)
    return MapCluster("zone:${zone.id}", zone.latitude, zone.longitude, zone.shopCount,
        MapBounds(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second }))
}
