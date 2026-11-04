package com.coffeepeek.feature.discovery.impl.map

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapCluster
import com.coffeepeek.feature.discovery.domain.model.MapZone
import kotlin.math.PI
import kotlin.math.cos

/** At group zooms, hiding zone overlays must still leave their shops represented on the map. */
internal fun MapState.canvasClusters(preview: Boolean): List<MapCluster> =
    clusters + if (showZones && !preview) emptyList() else zoneGroups.filter { it.shopCount > 0 }.map { zone ->
        val points = zone.clusterBoundsPoints()
        MapCluster("zone:${zone.id}", zone.center, zone.shopCount,
            MapBounds(points.minOf { it.latitude }, points.minOf { it.longitude }, points.maxOf { it.latitude }, points.maxOf { it.longitude }))
    }

private fun MapZone.clusterBoundsPoints(): List<Coordinates> {
    if (polygon.size >= 3) return polygon
    val latitude = radiusMeters / 111_320.0
    val longitude = latitude / cos(center.latitude * PI / 180).coerceAtLeast(0.01)
    return listOf(
        Coordinates((center.latitude - latitude).coerceAtLeast(-85.0), (center.longitude - longitude).coerceAtLeast(-180.0)),
        Coordinates((center.latitude + latitude).coerceAtMost(85.0), (center.longitude + longitude).coerceAtMost(180.0)),
    )
}
