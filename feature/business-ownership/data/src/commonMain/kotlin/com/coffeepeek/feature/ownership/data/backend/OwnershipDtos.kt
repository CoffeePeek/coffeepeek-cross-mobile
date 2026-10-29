package com.coffeepeek.feature.ownership.data.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable internal data class OwnershipEnvelope<T>(val isSuccess: Boolean = false, val message: String? = null, val data: T? = null)
@Serializable internal data class OwnershipPageDto<T>(val items: List<T> = emptyList(), val totalCount: Int = 0, val page: Int = 1, val pageSize: Int = 20)
@Serializable internal data class ClaimDto(
    val id: String, val businessType: String, val businessId: String, val businessNameSnapshot: String,
    val businessSlugSnapshot: String, val contact: String, val evidence: String, val status: String,
    val createdAtUtc: String, val decisionReason: String? = null, val reviewedAtUtc: String? = null,
    val hasCurrentAccess: Boolean = false, val evidencePhotoIds: List<String> = emptyList(),
)
@Serializable internal data class ClaimRequest(val businessType: String, val businessSlug: String, val contact: String, val evidence: String, val evidencePhotoIds: List<String> = emptyList())
@Serializable internal data class EvidenceUploadDto(val photoId: String, val storageKey: String)
@Serializable internal data class OwnerSummaryDto(val id: String, val name: String, val publicSlug: String? = null)
@Serializable internal data class OwnerShopsDto(val items: List<OwnerSummaryDto> = emptyList())
@Serializable internal data class ProfileDto(
    val id: String, val name: String, val profileRevision: Long, val description: String? = null,
    val about: String? = null, val cityId: String? = null, val location: LocationDto? = null,
    val contacts: ContactsDto? = null, val contact: ContactsDto? = null, val photos: List<PhotoDto> = emptyList(),
)
@Serializable internal data class LocationDto(val cityId: String? = null, val address: String? = null, val latitude: Double? = null, val longitude: Double? = null)
@Serializable internal data class ContactsDto(val phoneNumber: String? = null, val email: String? = null, val siteLink: String? = null, val instagramLink: String? = null)
@Serializable internal data class PhotoDto(val id: String? = null, val storageKey: String, val fileName: String = "photo.jpg",
    val fullUrl: String? = null, val contentType: String? = null, val sizeBytes: Long = 0)
@Serializable internal data class PhotoMetadataDto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
@Serializable internal data class ShopWriteDto(val name: String, val description: String?, val phoneNumber: String?,
    val email: String?, val siteLink: String?, val instagramLink: String?, val location: LocationDto,
    val schedules: List<JsonObject>? = null, val catalogs: JsonObject? = null)
@Serializable internal data class RoasterWriteDto(val name: String, val about: String?, val cityId: String?, val address: String?,
    val latitude: Double?, val longitude: Double?, val instagramLink: String?, val siteLink: String?, val photos: List<PhotoMetadataDto>? = null)
@Serializable internal data class PhotosRequest(val photos: List<PhotoMetadataDto>)
@Serializable internal data class PhotoIdsRequest(val photoIds: List<String>)
