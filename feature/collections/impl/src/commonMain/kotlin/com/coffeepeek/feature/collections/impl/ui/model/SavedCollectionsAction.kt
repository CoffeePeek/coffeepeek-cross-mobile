package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.*

internal sealed interface SavedCollectionsAction {
    data object Reload : SavedCollectionsAction
    data object Back : SavedCollectionsAction
    data object SignIn : SavedCollectionsAction
    data object Save : SavedCollectionsAction
    data object Delete : SavedCollectionsAction
    data object CommitMemberships : SavedCollectionsAction
    data object ShareLink : SavedCollectionsAction
    data object Preview : SavedCollectionsAction
    data object ShareImages : SavedCollectionsAction
    data class ExportFinished(val epoch: Int, val failed: Boolean) : SavedCollectionsAction
    data class OpenEditor(val id: String?) : SavedCollectionsAction
    data class OpenPlace(val place: CollectionPlace) : SavedCollectionsAction
    data class Title(val value: String) : SavedCollectionsAction
    data class Description(val value: String) : SavedCollectionsAction
    data class Visibility(val value: SavedCollectionVisibility) : SavedCollectionsAction
    data class Query(val value: String) : SavedCollectionsAction
    data class CoverPicked(val value: CollectionCoverInput, val accountVersion: Int) : SavedCollectionsAction
    data class CoverPreparing(val value: Boolean, val accountVersion: Int) : SavedCollectionsAction
    data object RemoveCover : SavedCollectionsAction
    data class TogglePlace(val key: String) : SavedCollectionsAction
    data class MovePlace(val key: String, val target: String, val after: Boolean) : SavedCollectionsAction
    data class ToggleMembership(val id: String) : SavedCollectionsAction
    data class Feature(val id: String?) : SavedCollectionsAction
    data class QuickTitle(val value: String?) : SavedCollectionsAction
    data object CreateQuick : SavedCollectionsAction
    data class Format(val value: CollectionPosterFormat) : SavedCollectionsAction
}

internal sealed interface SavedCollectionsEvent {
    data object Back : SavedCollectionsEvent
    data object SignIn : SavedCollectionsEvent
    data object CancelExport : SavedCollectionsEvent
    data class Editor(val id: String?) : SavedCollectionsEvent
    data class Place(val value: CollectionPlace) : SavedCollectionsEvent
    data class Share(val value: String) : SavedCollectionsEvent
    data class Export(val value: SharedCollection, val format: CollectionPosterFormat, val epoch: Int) : SavedCollectionsEvent
}
