package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapShop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapClustererTest {

    @Test
    fun countLabelCapsAt99Plus() {
        assertEquals("1", clusterCountLabel(1))
        assertEquals("99", clusterCountLabel(99))
        assertEquals("99+", clusterCountLabel(100))
        assertEquals("99+", clusterCountLabel(250))
    }

    @Test
    fun onlySingleShopClusterUsesShopPoint() {
        assertTrue(clusterUsesShopPoint(1))
        assertFalse(clusterUsesShopPoint(0))
        assertFalse(clusterUsesShopPoint(2))
    }

    @Test
    fun clusterDiameterByCount() {
        assertEquals(31f, clusterDiameterDp(1))
        assertEquals(34.1f, clusterDiameterDp(9))
        assertEquals(34.1f, clusterDiameterDp(10))
        assertEquals(34.1f, clusterDiameterDp(99))
        assertEquals(34.1f, clusterDiameterDp(100))
    }

    @Test
    fun noClustersAtOrAboveMinZoom() {
        val shops = listOf(
            shop("a", 53.9, 27.5),
            shop("b", 53.9, 27.5),
        )
        val items = clusterMapShops(shops, zoom = 15f) { ScreenXy(0f, 0f) }
        assertEquals(2, items.size)
        assertTrue(items.all { it is MapMarkerItem.Shop })
    }

    @Test
    fun closePointsClusterBelowMinZoom() {
        val shops = listOf(
            shop("a", 53.90, 27.50),
            shop("b", 53.91, 27.51),
        )
        val items = clusterMapShops(shops, zoom = 12f) { shop ->
            if (shop.id == "a") ScreenXy(0f, 0f) else ScreenXy(20f, 0f)
        }
        assertEquals(1, items.size)
        val cluster = assertIs<MapMarkerItem.Cluster>(items.single())
        assertEquals(2, cluster.count)
        assertEquals("2", cluster.label)
        assertEquals(53.905, cluster.latitude, 0.0001)
    }

    @Test
    fun distantPointsStaySeparate() {
        val shops = listOf(
            shop("a", 53.90, 27.50),
            shop("b", 53.91, 27.51),
        )
        val items = clusterMapShops(shops, zoom = 12f) { shop ->
            if (shop.id == "a") ScreenXy(0f, 0f) else ScreenXy(50f, 0f)
        }
        assertEquals(2, items.size)
        assertTrue(items.all { it is MapMarkerItem.Shop })
    }

    @Test
    fun clusterFitZoomDoesNotExceedCap() {
        assertEquals(14f, clusterFitZoom(currentZoom = 12f, fittedZoom = 16f))
        assertEquals(18f, clusterFitZoom(currentZoom = 16f, fittedZoom = 20f))
        assertEquals(13f, clusterFitZoom(currentZoom = 12f, fittedZoom = 13f))
        assertEquals(13f, clusterFitZoom(currentZoom = 12f, fittedZoom = 9f))
        assertEquals(22f, clusterFitZoom(currentZoom = 21.5f, fittedZoom = 25f))
    }

    private fun shop(id: String, lat: Double, lon: Double) = MapShop(
        id = id,
        title = id,
        latitude = lat,
        longitude = lon,
    )
}
