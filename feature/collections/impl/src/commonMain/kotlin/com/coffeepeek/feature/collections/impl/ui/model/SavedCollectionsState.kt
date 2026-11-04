package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.*

internal enum class SavedCollectionsMode { Library, Editor, Picker, Preference, Public, Featured }
internal enum class SavedCollectionsFailure { Load, Save, Conflict, Cover, Membership, Delete }

internal data class SavedCollectionsState(
    val mode: SavedCollectionsMode,
    val accountVersion: Int = 0,
    val signedIn: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val listsLoaded: Boolean = !loading,
    val busy: Boolean = false,
    val failure: SavedCollectionsFailure? = null,
    val lists: List<SavedCollectionSummary> = emptyList(),
    val collection: SavedCollection? = null,
    val title: String = "",
    val description: String = "",
    val visibility: SavedCollectionVisibility = SavedCollectionVisibility.Private,
    val coverInput: CollectionCoverInput? = null,
    val uploadedCover: CollectionCoverUpload? = null,
    val removeCover: Boolean = false,
    val preparingCover: Boolean = false,
    val query: String = "",
    val quickTitle: String? = null,
    val places: List<CollectionPlace> = emptyList(),
    val selectedPlaces: Set<String> = emptySet(),
    val memberships: Set<String> = emptySet(),
    val membershipsLoaded: Boolean = false,
    val featuredId: String? = null,
    val preview: Boolean = false,
    val format: CollectionPosterFormat = CollectionPosterFormat.Chat,
)

internal fun SavedCollectionsState.hasEdits(): Boolean {
    val original = collection ?: return true
    return title != original.summary.title || description != original.summary.description ||
        visibility != original.summary.visibility || coverInput != null || removeCover ||
        places.filter { it.key in selectedPlaces }.map { it.key } != original.places.map { it.key }
}

internal fun SavedCollection.toSharedPoster(): SharedCollection =
    SharedCollection(summary.id, summary.title,
        summary.url.takeIf { summary.visibility == SavedCollectionVisibility.Public }.orEmpty(), places)
