package com.coffeepeek.domain.model

enum class ShopChangeSection {
    Photos,
    Contacts,
    Description,
    Tags,
    Roasters,
    Equipment,
    Menu,
    BrewMethods,
}

enum class ModerationStatus {
    Pending,
    Approved,
    Rejected,
}

enum class MenuItemAvailability {
    Unknown,
    Present,
    Absent,
}

data class ShopPhoto(
    val id: String,
    val fullUrl: String,
    val previewUrl: String = fullUrl,
    val sortIndex: Int = 0,
)

data class ShopChangeContacts(
    val phoneNumber: String? = null,
    val email: String? = null,
    val siteLink: String? = null,
    val instagramLink: String? = null,
)

data class ShopChangeGallery(
    val retainedPhotoIds: List<String> = emptyList(),
    val newPhotos: List<UploadedPhotoMeta> = emptyList(),
)

data class ShopChangeMenuItem(
    val slug: String,
    val availability: MenuItemAvailability,
    val price: Double? = null,
    val volumeMl: Int? = null,
)

data class ShopChangeMenu(
    val items: List<ShopChangeMenuItem> = emptyList(),
    val retainedPhotoIds: List<String> = emptyList(),
    val newPhotos: List<UploadedPhotoMeta> = emptyList(),
)

data class ShopChangePayload(
    val description: String? = null,
    val contacts: ShopChangeContacts? = null,
    val photos: ShopChangeGallery? = null,
    val tagIds: List<String>? = null,
    val roasterIds: List<String>? = null,
    val equipmentIds: List<String>? = null,
    val menu: ShopChangeMenu? = null,
    val brewMethodIds: List<String>? = null,
)

data class ShopChangeRequest(
    val id: String,
    val shopId: String,
    val section: ShopChangeSection,
    val payload: ShopChangePayload,
    val status: ModerationStatus,
    val reviewedAtUtc: String? = null,
    val rejectionReason: String? = null,
    val createdAtUtc: String,
    val updatedAtUtc: String? = null,
)

data class ShopChangeDraft(
    val shopId: String,
    val section: ShopChangeSection,
    val description: String? = null,
    val contacts: ShopChangeContacts? = null,
    val retainedPhotoIds: List<String> = emptyList(),
    val newPhotos: List<PendingPhotoUpload> = emptyList(),
    val alreadyUploadedPhotos: List<UploadedPhotoMeta> = emptyList(),
    val tagIds: List<String>? = null,
    val roasterIds: List<String>? = null,
    val equipmentIds: List<String>? = null,
    val brewMethodIds: List<String>? = null,
    val menuItems: List<ShopChangeMenuItem>? = null,
    val retainedMenuPhotoIds: List<String> = emptyList(),
    val newMenuPhotos: List<PendingPhotoUpload> = emptyList(),
    val alreadyUploadedMenuPhotos: List<UploadedPhotoMeta> = emptyList(),
)
