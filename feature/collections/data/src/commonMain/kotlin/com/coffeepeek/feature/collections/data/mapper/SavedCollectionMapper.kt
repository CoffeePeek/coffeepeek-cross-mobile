package com.coffeepeek.feature.collections.data.mapper

import com.coffeepeek.feature.collections.data.backend.*
import com.coffeepeek.feature.collections.domain.model.*

internal fun SavedCollectionDto.toSavedSummary(owned: Boolean): SavedCollectionSummary {
    check(mode == "list")
    val visibility = when (visibility) {
        "public" -> SavedCollectionVisibility.Public
        "private" -> SavedCollectionVisibility.Private
        else -> error("Unknown collection visibility")
    }
    check(owned || visibility == SavedCollectionVisibility.Public)
    return SavedCollectionSummary(id, title, description.orEmpty(), visibility,
        coverPhoto?.let { SavedCollectionCover(it.id, owned) }, itemCount.coerceAtLeast(0), revision,
        containsItem, isFeatured, canonicalPath?.takeIf { visibility == SavedCollectionVisibility.Public }?.let { collectionPublicUrl(id, it) },
        isDefault = isDefault)
}

internal fun SavedCollectionDto.toSavedCollection(owned: Boolean, apiBaseUrl: String): SavedCollection =
    SavedCollection(toSavedSummary(owned), CollectionRules.orderedPlaces(items.mapNotNull { collectionPlace(it, apiBaseUrl) }))

internal fun SavedCollectionMetadata.toRequest(revision: Long? = null, new: Boolean = false): SavedMetadataDto =
    SavedMetadataDto(title.trim(), description.trim().ifBlank { null },
        if (visibility == SavedCollectionVisibility.Public) "public" else "private",
        coverUpload?.let { SavedCoverUploadDto(it.fileName, it.contentType, it.storageKey, it.size) }, removeCover,
        revision, if (new) emptyList() else null)

internal fun List<CollectionPlace>.toSavedItems(): List<CollectionSelectionDto> =
    map { CollectionSelectionDto(collectionWireKind(it.kind), it.slug) }
