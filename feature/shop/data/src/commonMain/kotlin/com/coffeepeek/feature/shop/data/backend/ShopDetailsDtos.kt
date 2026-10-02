@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.shop.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonElement

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
    @SerialName("schedules") val schedules: List<ShopScheduleDto>? = null,
)

@Serializable
internal data class ShopLocationDto(
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

@Serializable
internal data class ShopMenuDto(
    @SerialName("capturedAtUtc") val capturedAtUtc: String? = null,
    @SerialName("updatedAtUtc") val updatedAtUtc: String? = null,
    @SerialName("currency") val currency: String = "BYN",
    @SerialName("items") val items: List<ShopMenuItemDto> = emptyList(),
    @SerialName("photos") val photos: List<ShopPhotoDto> = emptyList(),
)

@Serializable
internal data class ShopMenuItemDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("availability") val availability: String = "Unknown",
    @SerialName("price") val price: JsonElement? = null,
    @SerialName("currency") val currency: String = "BYN",
    @SerialName("volumeMl") val volumeMl: JsonElement? = null,
)

@Serializable
internal data class ShopScheduleDto(
    @SerialName("dayOfWeek") val dayOfWeek: JsonElement? = null,
    @SerialName("isClosed") val isClosed: Boolean = false,
    @SerialName("intervals") val intervals: List<ShopScheduleIntervalDto>? = null,
)

@Serializable
internal data class ShopScheduleIntervalDto(
    @SerialName("openTime") val openTime: String = "",
    @SerialName("closeTime") val closeTime: String = "",
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
