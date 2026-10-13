package com.coffeepeek.admin.map

import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapShop
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Presentation payload for the Swift map renderer, kept here so its contract is testable. */
internal fun nativeMapStateJson(
    shops: List<MapShop>,
    clusters: List<MapCluster>,
    zones: List<MapCoffeeZone>,
    selectedShopId: String?,
    isDarkTheme: Boolean,
    showAttribution: Boolean,
): String = buildJsonObject {
    put("selectedShopId", selectedShopId?.let(::JsonPrimitive) ?: JsonNull)
    put("dark", isDarkTheme)
    put("showAttribution", showAttribution)
    put("shops", buildJsonArray {
        shops.forEach { shop -> add(buildJsonObject {
            put("id", shop.id)
            put("title", shop.title)
            put("lat", shop.latitude)
            put("lon", shop.longitude)
            put("type", shop.type)
            put("selected", shop.id == selectedShopId)
        }) }
    })
    put("clusters", buildJsonArray {
        clusters.forEach { cluster -> add(buildJsonObject {
            put("id", cluster.id)
            put("lat", cluster.latitude)
            put("lon", cluster.longitude)
            put("count", cluster.count)
            put("bounds", buildJsonObject {
                put("minLat", cluster.bounds.minLat)
                put("minLon", cluster.bounds.minLon)
                put("maxLat", cluster.bounds.maxLat)
                put("maxLon", cluster.bounds.maxLon)
            })
        }) }
    })
    put("zones", buildJsonArray {
        zones.forEach { zone -> add(buildJsonObject {
            put("id", zone.id)
            put("name", zone.name)
            put("color", zoneColorForMap(zone.color, isDarkTheme))
            put("lat", zone.latitude)
            put("lon", zone.longitude)
            put("count", zone.shopCount)
            put("polygon", buildJsonArray {
                mapZoneOutline(zone).forEach { (lat, lon) ->
                    add(buildJsonArray { add(lat); add(lon) })
                }
            })
        }) }
    })
}.toString()

/** Closed latitude/longitude ring, with the same radius fallback on both platforms. */
internal fun mapZoneOutline(zone: MapCoffeeZone): List<Pair<Double, Double>> {
    if (zone.polygon.size >= 3) {
        return if (zone.polygon.first() == zone.polygon.last()) zone.polygon
        else zone.polygon + zone.polygon.first()
    }
    val latitudeDegrees = zone.radiusMeters / 111_320.0
    val longitudeDegrees = latitudeDegrees / cos(zone.latitude * PI / 180.0).coerceAtLeast(0.01)
    val points = (0 until 48).map { index ->
        val angle = 2.0 * PI * index / 48
        (zone.latitude + latitudeDegrees * sin(angle)) to
            (zone.longitude + longitudeDegrees * cos(angle))
    }
    return points + points.first()
}
