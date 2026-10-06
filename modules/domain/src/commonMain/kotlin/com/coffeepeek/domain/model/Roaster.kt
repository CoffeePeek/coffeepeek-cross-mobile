package com.coffeepeek.domain.model

data class RoasterDetails(
    val id: String,
    val name: String,
    val about: String? = null,
    val location: RoasterLocation? = null,
    val contact: RoasterContact? = null,
    val photos: List<RoasterPhoto> = emptyList(),
    val shops: List<RoasterShop> = emptyList(),
    val publicAddress: PublicAddress? = null,
)

data class RoasterLocation(
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class RoasterContact(
    val instagramLink: String? = null,
    val siteLink: String? = null,
)

data class RoasterPhoto(
    val id: String,
    val fileName: String,
    val storageKey: String,
    val fullUrl: String,
    val sortIndex: Int = 0,
)

data class RoasterShop(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val publicAddress: PublicAddress? = null,
)

data class CreateRoasterInput(
    val name: String,
    val about: String? = null,
    val cityId: String? = null,
    val address: String? = null,
    val instagramLink: String? = null,
    val siteLink: String? = null,
    val photos: List<PendingPhotoUpload> = emptyList(),
)

data class RoasterSubmissionResult(
    val roasterId: String,
    val status: String,
    val isAddressValidated: Boolean,
    val message: String,
)

data class RoasterSubmission(
    val id: String,
    val name: String,
    val about: String?,
    val status: ModerationStatus,
    val rejectedReason: String?,
)
