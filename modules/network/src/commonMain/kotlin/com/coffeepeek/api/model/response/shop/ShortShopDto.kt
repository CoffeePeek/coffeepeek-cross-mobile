package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.DataResponse
import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.serialization.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ShortShopDto(
    @SerialName("address") val address: PublicAddressDto,
    @SerialName("city") val city: PublicAddressDto? = null,
    @SerialName("name") val name: String,
    @SerialName("photos") val photos: List<ShortPhotoDto> = emptyList(),
    @SerialName("rating") val rating: Double = 0.0,
    @Serializable(with = FlexibleIntSerializer::class) val checkInCount: Int = 0,
    @SerialName("isFavorite") val isFavorite: Boolean = false,
    @SerialName("isVisited") val isVisited: Boolean = false,
    @SerialName("isNew") val isNew: Boolean = false,
    @SerialName("isOpen") val isOpen: Boolean = false,
    @SerialName("priceRange") val priceRange: JsonElement? = null,
    @SerialName("type") val type: JsonElement? = null,
    @SerialName("coffeeFocus") val coffeeFocus: JsonElement? = null,
    @SerialName("location") val location: LocationDto? = null,
    @SerialName("beans") val beans: List<CatalogItemDto> = emptyList(),
    @SerialName("roasters") val roasters: List<CatalogItemDto> = emptyList(),
    @SerialName("equipments") val equipments: List<CatalogItemDto> = emptyList(),
    @SerialName("brewMethods") val brewMethods: List<CatalogItemDto> = emptyList(),
    @SerialName("tags") val tags: JsonElement? = null,
    @SerialName("shopTags") val shopTags: JsonElement? = null,
)

@Serializable
data class GetShopsResponseDto(
    @SerialName("coffeeShops") val coffeeShops: List<ShortShopDto> = emptyList(),
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("pageSize") val pageSize: Int = 10,
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
) : DataResponse()
