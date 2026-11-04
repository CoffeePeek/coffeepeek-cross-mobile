package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary

internal sealed interface SavedOwnerProfileContent {
    data object Status : SavedOwnerProfileContent
    data object Empty : SavedOwnerProfileContent
    data class Lists(val values: List<SavedCollectionSummary>) : SavedOwnerProfileContent
}

/** Render only a verified snapshot for the active account, including during a failed refresh. */
internal fun savedOwnerProfileContent(state: SavedCollectionsState): SavedOwnerProfileContent {
    if (!state.signedIn || !state.listsLoaded) return SavedOwnerProfileContent.Status
    return if (state.lists.isEmpty()) SavedOwnerProfileContent.Empty else SavedOwnerProfileContent.Lists(state.lists)
}
