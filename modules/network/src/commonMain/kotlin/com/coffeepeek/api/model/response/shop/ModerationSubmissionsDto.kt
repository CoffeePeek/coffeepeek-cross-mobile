package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.DataResponse
import com.coffeepeek.api.model.request.ModerationStatusDto
import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Only the fields the "my contributions" lists render; the rest of the moderation DTOs is ignored.

@Serializable
data class ModerationShopDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("address") val address: String? = null,
    @SerialName("moderationStatus") val moderationStatus: ModerationStatusDto,
    @SerialName("rejectedReason") val rejectedReason: String? = null,
    @SerialName("publishedShop") val publishedShop: PublicAddressDto? = null,
)

@Serializable
data class MyModerationShopsPageDto(
    @SerialName("items") val moderationShops: List<ModerationShopDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
) : DataResponse()

@Serializable
data class ModerationRoasterDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("about") val about: String? = null,
    @SerialName("moderationStatus") val moderationStatus: ModerationStatusDto,
    @SerialName("rejectedReason") val rejectedReason: String? = null,
)

@Serializable
data class MyModerationRoastersPageDto(
    @SerialName("items") val items: List<ModerationRoasterDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
) : DataResponse()
