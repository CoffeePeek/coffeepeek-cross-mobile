package com.coffeepeek.feature.collections.domain.model

enum class SavedCollectionVisibility { Private, Public }

data class SavedCollectionTarget(val kind: CollectionPlaceKind, val slug: String)

/** Private media is loaded by collection data, never by an anonymous image loader. */
data class SavedCollectionCover(val id: String, val authenticated: Boolean)

data class SavedCollectionSummary(
    val id: String,
    val title: String,
    val description: String = "",
    val visibility: SavedCollectionVisibility = SavedCollectionVisibility.Private,
    val cover: SavedCollectionCover? = null,
    val itemCount: Int = 0,
    val revision: Long = 1,
    val containsItem: Boolean = false,
    val isFeatured: Boolean = false,
    val url: String? = null,
)

data class SavedCollection(val summary: SavedCollectionSummary, val places: List<CollectionPlace> = emptyList())
data class CollectionCoverInput(val bytes: ByteArray, val mimeType: String, val fileName: String)
data class CollectionCoverUpload(val fileName: String, val contentType: String, val storageKey: String, val size: Long)

data class SavedCollectionMetadata(
    val title: String,
    val description: String = "",
    val visibility: SavedCollectionVisibility = SavedCollectionVisibility.Private,
    val coverUpload: CollectionCoverUpload? = null,
    val removeCover: Boolean = false,
)

class SavedCollectionConflict : IllegalStateException("Collection revision changed")

object SavedCollectionRules {
    const val maxDescriptionLength = 1000
    const val maxCoverBytes = 1_048_576

    fun validMetadata(value: SavedCollectionMetadata): Boolean = CollectionRules.validTitle(value.title) &&
        value.description.length <= maxDescriptionLength && value.description.none { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }

    fun validCover(value: CollectionCoverInput): Boolean = value.bytes.size in 1..maxCoverBytes &&
        value.mimeType in setOf("image/jpeg", "image/png", "image/webp") && value.fileName.isNotBlank()
}
