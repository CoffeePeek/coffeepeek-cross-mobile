package com.coffeepeek.feature.discovery.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopMapRequestDto(
    @SerialName("q") val q: String,
    @SerialName("filters") val filters: ShopSearchFiltersDto,
    @SerialName("bounds") val bounds: MapBoundsDto,
    @SerialName("zoom") val zoom: Int,
)

@Serializable
internal data class MapBoundsDto(
    @SerialName("minLat") val minLat: Double,
    @SerialName("minLon") val minLon: Double,
    @SerialName("maxLat") val maxLat: Double,
    @SerialName("maxLon") val maxLon: Double,
)
