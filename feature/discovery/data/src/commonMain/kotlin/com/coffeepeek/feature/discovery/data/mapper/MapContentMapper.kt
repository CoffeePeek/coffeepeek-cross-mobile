package com.coffeepeek.feature.discovery.data.mapper

import com.coffeepeek.feature.discovery.data.backend.ClusterDto
import com.coffeepeek.feature.discovery.data.backend.MapDto
import com.coffeepeek.feature.discovery.data.backend.MapShopDto
import com.coffeepeek.feature.discovery.data.backend.ZoneDto
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapCluster
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun MapDto.toContent(kind: ShopKind?) = MapContent(
    markers = shops.map { it.toMarker() }.filter { kind == null || it.kind == kind },
    zones = zones.map { it.toZone() },
    isTruncated = isTruncated,
    clusters = clusters.mapNotNull { it.toCluster() },
)

private fun MapShopDto.toMarker() = MapMarker(
    id = address.slug,
    title = title?.takeIf(String::isNotBlank) ?: UNNAMED_SHOP,
    coordinates = Coordinates(latitude, longitude),
    kind = ShopKind.fromApi((type as? JsonPrimitive)?.contentOrNull?.trim()),
    zoneId = primaryZone?.slug,
)

private fun ZoneDto.toZone() = MapZone(
    id = address.slug, name = name, description = description, color = color,
    center = Coordinates(latitude, longitude), radiusMeters = radiusMeters, shopCount = shopCount,
    polygon = polygon.map { Coordinates(it.latitude, it.longitude) },
)

private fun ClusterDto.toCluster(): MapCluster? {
    val center = Coordinates(latitude, longitude)
    val lower = Coordinates(bounds.minLatitude, bounds.minLongitude)
    val upper = Coordinates(bounds.maxLatitude, bounds.maxLongitude)
    if (count < 1 || !center.isValid || !lower.isValid || !upper.isValid) return null
    if (lower.latitude > upper.latitude || lower.longitude > upper.longitude) return null
    return MapCluster(
        id = id?.takeIf(String::isNotBlank) ?: "$latitude:$longitude:$count",
        coordinates = center, count = count,
        bounds = MapBounds(lower.latitude, lower.longitude, upper.latitude, upper.longitude),
    )
}
