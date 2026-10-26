@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.api.model.request

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class ShopSearchRequestDto(
    val filters: ShopSearchFiltersDto,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sort: String? = null,
    val page: Int,
    val pageSize: Int,
    // Gateway rejects null and blank q; omit it when there is no text query.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val q: String? = null,
)

@Serializable
data class ShopSearchFiltersDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val origin: ShopSearchOriginDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val radiusKm: Int? = null,
    val city: String? = null,
    val tags: List<String> = emptyList(),
    val roasters: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val beans: List<String> = emptyList(),
    val brewMethods: List<String> = emptyList(),
    val isOpen: Boolean? = null,
    val isNew: Boolean? = null,
    val visitedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val minRating: Double? = null,
    val priceRange: String? = null,
    val type: String? = null,
)

@Serializable
data class ShopSearchOriginDto(val latitude: Double, val longitude: Double)
