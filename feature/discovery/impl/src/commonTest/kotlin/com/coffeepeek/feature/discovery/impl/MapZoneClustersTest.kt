package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.canvasClusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapZoneClustersTest {
    private val zone = MapZone("centre", "Centre", "", null, Coordinates(53.9, 27.56), 800.0, 12)

    @Test fun hiddenOverlaysAndPreviewStillRepresentZoneShopsWithClickableGroups() {
        val state = MapState(zoneGroups = listOf(zone))
        assertTrue(state.canvasClusters(preview = false).isEmpty())
        val cluster = state.canvasClusters(preview = true).single()
        assertEquals(12, cluster.count)
        assertEquals(zone.center, cluster.coordinates)
        assertTrue(cluster.bounds.minLat < zone.center.latitude)
        assertTrue(cluster.bounds.maxLon > zone.center.longitude)
        assertEquals(listOf(cluster), state.copy(showZones = false).canvasClusters(preview = false))
    }

    @Test fun polygonBoundsAreUsedAndOldOverlayCacheDoesNotBecomeAGroupAtShopZoom() {
        val polygon = listOf(Coordinates(1.0, 2.0), Coordinates(2.0, 4.0), Coordinates(3.0, 3.0))
        val state = MapState(zoneGroups = listOf(zone.copy(polygon = polygon), zone.copy(id = "empty", shopCount = 0)))
        assertEquals(MapBounds(1.0, 2.0, 3.0, 4.0), state.canvasClusters(true).single().bounds)
        assertTrue(MapState(zones = listOf(zone), showZones = false).canvasClusters(false).isEmpty())
    }
}
