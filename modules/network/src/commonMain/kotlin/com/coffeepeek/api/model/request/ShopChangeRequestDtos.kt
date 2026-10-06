package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.DataResponse
import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ShopChangeSectionDto {
    Photos,
    Contacts,
    Description,
    Tags,
    Roasters,
    Equipment,
    Menu,
    BrewMethods,
}

@Serializable
enum class ModerationStatusDto {
    Pending,
    Approved,
    Rejected,
}

@Serializable
enum class MenuItemAvailabilityDto {
    Unknown,
    Present,
    Absent,
}

@Serializable
data class ShopChangeContactsDto(
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
    @SerialName("instagramLink") val instagramLink: String? = null,
)

@Serializable
data class ShopChangeGalleryDto(
    @SerialName("retainedPhotoIds") val retainedPhotoIds: List<String> = emptyList(),
    @SerialName("newPhotos") val newPhotos: List<UploadedPhotoReq> = emptyList(),
)

@Serializable
data class UpdateShopMenuItemRequestDto(
    @SerialName("slug") val slug: String,
    @SerialName("availability") val availability: MenuItemAvailabilityDto,
    @SerialName("price") val price: Double? = null,
    @SerialName("volumeMl") val volumeMl: Int? = null,
)

@Serializable
data class ShopChangeMenuDto(
    @SerialName("items") val items: List<UpdateShopMenuItemRequestDto> = emptyList(),
    @SerialName("retainedPhotoIds") val retainedPhotoIds: List<String> = emptyList(),
    @SerialName("newPhotos") val newPhotos: List<UploadedPhotoReq> = emptyList(),
)

@Serializable
data class ShopChangePayloadDto(
    @SerialName("description") val description: String? = null,
    @SerialName("contacts") val contacts: ShopChangeContactsDto? = null,
    @SerialName("photos") val photos: ShopChangeGalleryDto? = null,
    @SerialName("tags") val tagIds: List<String>? = null,
    @SerialName("roasters") val roasterIds: List<String>? = null,
    @SerialName("equipments") val equipmentIds: List<String>? = null,
    @SerialName("menu") val menu: ShopChangeMenuDto? = null,
    @SerialName("brewMethods") val brewMethodIds: List<String>? = null,
)

@Serializable
data class CreateShopChangeRequestBody(
    @SerialName("shop") val shopId: String,
    @SerialName("section") val section: ShopChangeSectionDto,
    @SerialName("payload") val payload: ShopChangePayloadDto,
)

@Serializable
data class UpdateShopChangeRequestBody(
    @SerialName("section") val section: ShopChangeSectionDto,
    @SerialName("payload") val payload: ShopChangePayloadDto,
)

@Serializable
data class ShopChangeRequestDto(
    @SerialName("id") val id: String,
    @SerialName("shop") val shop: PublicAddressDto? = null,
    @SerialName("section") val section: ShopChangeSectionDto,
    @SerialName("payload") val payload: ShopChangePayloadDto,
    @SerialName("status") val status: ModerationStatusDto,
    @SerialName("reviewedAtUtc") val reviewedAtUtc: String? = null,
    @SerialName("rejectionReason") val rejectionReason: String? = null,
    @SerialName("createdAtUtc") val createdAtUtc: String,
    @SerialName("updatedAtUtc") val updatedAtUtc: String? = null,
) : DataResponse()

@Serializable
data class ShopChangeRequestPageDto(
    @SerialName("items") val items: List<ShopChangeRequestDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("pageSize") val pageSize: Int = 20,
) : DataResponse()
