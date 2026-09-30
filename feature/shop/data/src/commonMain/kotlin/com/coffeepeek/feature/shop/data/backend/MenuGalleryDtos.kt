@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.shop.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class MenuGalleryResponse(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean = false,
    @SerialName("data") @JsonNames("Data") val data: MenuGalleryData? = null,
)

@Serializable
internal data class MenuGalleryData(
    @SerialName("shopDto") val shop: GalleryShopDto,
    @SerialName("menu") val menu: GalleryMenuDto? = null,
)

@Serializable
internal data class GalleryShopDto(
    @SerialName("name") val name: String? = null,
    @SerialName("menu") val menu: GalleryMenuDto? = null,
)

@Serializable
internal data class GalleryMenuDto(
    @SerialName("photos") val photos: List<GalleryPhotoDto> = emptyList(),
)

@Serializable
internal data class GalleryPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("urls") val urls: GalleryPhotoUrlsDto? = null,
    @SerialName("sortIndex") val sortIndex: Int = 0,
)

@Serializable
internal data class GalleryPhotoUrlsDto(
    @SerialName("detail") val detail: String? = null,
    @SerialName("fullscreen") val fullscreen: String? = null,
)
