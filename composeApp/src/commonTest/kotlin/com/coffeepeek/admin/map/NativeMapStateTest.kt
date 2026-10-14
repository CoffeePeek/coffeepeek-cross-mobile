package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.cos
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeMapStateTest {
    @Test
    fun payloadPreservesSelectionZoneCountAndClusterBounds() {
        val title = "Кофейня \"Угол\"\n\t\u0001"
        val payload = Json.parseToJsonElement(nativeMapStateJson(
            shops = listOf(MapShop("shop", title, 53.9, 27.5)),
            clusters = listOf(MapCluster("cluster", 53.9, 27.5, 12, MapBounds(53.8, 27.4, 54.0, 27.6))),
            zones = listOf(zone.copy(color = "#f80", shopCount = 9)),
            selectedShopId = "shop", isDarkTheme = true, showAttribution = false,
        )).jsonObject
        val shop = payload.getValue("shops").jsonArray.single().jsonObject
        assertEquals(title, shop.getValue("title").jsonPrimitive.content)
        assertTrue(shop.getValue("selected").jsonPrimitive.boolean)
        assertFalse(payload.getValue("showAttribution").jsonPrimitive.boolean)
        val label = payload.getValue("zones").jsonArray.single().jsonObject
        assertEquals(9, label.getValue("count").jsonPrimitive.int)
        assertEquals("#FF8800", label.getValue("color").jsonPrimitive.content)
        assertEquals(49, label.getValue("polygon").jsonArray.size)
        val cluster = payload.getValue("clusters").jsonArray.single().jsonObject
        assertEquals(27.6, cluster.getValue("bounds").jsonObject.getValue("maxLon").jsonPrimitive.double)
    }

    @Test
    fun polygonKeepsLatitudeLongitudeOrderAndClosesOnlyOnce() {
        val vertices = listOf(53.90 to 27.50, 53.91 to 27.51, 53.92 to 27.49)
        val outline = mapZoneOutline(zone.copy(polygon = vertices))
        assertEquals(vertices + vertices.first(), outline)
        assertEquals(outline, mapZoneOutline(zone.copy(polygon = outline)))
    }

    @Test
    fun radiusFallbackPreservesMetersAndClosesRingExactly() {
        val outline = mapZoneOutline(zone)
        assertEquals(49, outline.size)
        assertEquals(outline.first(), outline.last())
        val north = outline[12]
        assertEquals(zone.radiusMeters, (north.first - zone.latitude) * 111_320.0, 0.001)
        val east = outline.first()
        assertEquals(zone.radiusMeters,
            (east.second - zone.longitude) * 111_320.0 * cos(zone.latitude * PI / 180.0), 0.001)
    }

    private val zone = MapCoffeeZone(
        id = "zone", name = "Немига", description = "", latitude = 53.9, longitude = 27.5,
        radiusMeters = 500.0, shopCount = 5,
    )
}
