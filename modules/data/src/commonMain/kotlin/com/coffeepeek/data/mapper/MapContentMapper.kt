package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.shop.GetShopsInBoundsResponseDto
import com.coffeepeek.api.model.response.shop.MapClusterDto
import com.coffeepeek.api.model.response.shop.MapCoffeeZoneDto
import com.coffeepeek.api.model.response.shop.MapShopDto
import com.coffeepeek.data.mapper.ShopMapper.parseShopType
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.MapShop

/** Compatibility for the retained iOS screen until its Discovery entry is migrated. */
internal fun GetShopsInBoundsResponseDto.toMapContent(focus: String?) = MapContent(
    shops = shops.map { it.toMapShop() }.filter { focus.isNullOrBlank() || it.type == focus },
    clusters = clusters.mapNotNull { it.toMapCluster() },
    zones = zones.map { it.toMapZone() },
    isTruncated = isTruncated,
)

private fun MapShopDto.toMapShop() = MapShop(
    id = address.slug, publicAddress = address.toDomain(),
    title = title?.takeIf(String::isNotBlank) ?: "Кофейня",
    latitude = latitude, longitude = longitude, type = parseShopType(type), primaryZoneId = primaryZone?.slug,
)

private fun MapCoffeeZoneDto.toMapZone() = MapCoffeeZone(
    id = address.slug, publicAddress = address.toDomain(), name = name, description = description, color = color,
    latitude = latitude, longitude = longitude, radiusMeters = radiusMeters, shopCount = shopCount,
    polygon = polygon.map { it.latitude to it.longitude },
)

private fun MapClusterDto.toMapCluster(): MapCluster? {
    if (count < 1 || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    if (bounds.minLatitude !in -90.0..90.0 || bounds.maxLatitude !in bounds.minLatitude..90.0) return null
    if (bounds.minLongitude !in -180.0..180.0 || bounds.maxLongitude !in bounds.minLongitude..180.0) return null
    return MapCluster(
        id = id?.takeIf(String::isNotBlank) ?: "$latitude:$longitude:$count",
        latitude = latitude, longitude = longitude, count = count,
        bounds = MapBounds(bounds.minLatitude, bounds.minLongitude, bounds.maxLatitude, bounds.maxLongitude),
    )
}
