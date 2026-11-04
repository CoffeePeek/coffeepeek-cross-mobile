package com.coffeepeek.feature.collections.data.backend

import kotlinx.serialization.Serializable

@Serializable internal data class SavedCoverDto(val id: String, val url: String = "")
@Serializable internal data class SavedCollectionDto(
    val id: String, val title: String, val description: String? = null, val visibility: String = "private",
    val coverPhoto: SavedCoverDto? = null, val itemCount: Int = 0, val revision: Long = 1,
    val containsItem: Boolean = false, val isFeatured: Boolean = false, val canonicalPath: String? = null,
    val items: List<CollectionPlaceDto> = emptyList(), val mode: String = "list",
    val isDefault: Boolean = false,
)
@Serializable internal data class SavedCoverUploadDto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
@Serializable internal data class SavedMetadataDto(
    val title: String, val description: String?, val visibility: String,
    val coverUpload: SavedCoverUploadDto? = null, val removeCover: Boolean = false,
    val expectedRevision: Long? = null, val items: List<CollectionSelectionDto>? = null,
)
@Serializable internal data class SavedItemsDto(val items: List<CollectionSelectionDto>, val expectedRevision: Long)
@Serializable internal data class SavedMembershipDto(val collectionIds: List<String>,
    val addedCollectionIds: List<String>? = null, val removedCollectionIds: List<String>? = null)
@Serializable internal data class SavedFeaturedDto(val collectionId: String?)
@Serializable internal data class SavedCoverReservationDto(val fileName: String, val contentType: String, val sizeBytes: Long)
@Serializable internal data class SavedCoverReservationResponse(val photoId: String, val uploadUrl: String, val storageKey: String)
