@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.shop.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class ShopDetailsResponse(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean = false,
    @SerialName("data") @JsonNames("Data") val data: ShopDetailsData? = null,
)

@Serializable
internal data class ShopDetailsData(
    @SerialName("shopDto") val shop: ShopDetailsDto,
    @SerialName("menu") val menu: ShopMenuDto? = null,
)

@Serializable
internal data class ShopDetailsDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: ShopLocationDto? = null,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("reviewCount") val reviewCount: Int = 0,
    @SerialName("isOpen") val isOpen: Boolean = false,
    @SerialName("photos") val photos: List<ShopPhotoDto> = emptyList(),
    @SerialName("menu") val menu: ShopMenuDto? = null,
)

@Serializable
internal data class ShopLocationDto(
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

@Serializable
internal data class ShopMenuDto(
    @SerialName("photos") val photos: List<ShopPhotoDto> = emptyList(),
)

@Serializable
internal data class ShopPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("urls") val urls: ShopPhotoUrlsDto? = null,
    @SerialName("sortIndex") val sortIndex: Int = 0,
)

@Serializable
internal data class ShopPhotoUrlsDto(
    @SerialName("detail") val detail: String? = null,
    @SerialName("fullscreen") val fullscreen: String? = null,
)
