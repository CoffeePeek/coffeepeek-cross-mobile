package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary

internal sealed interface SavedOwnerProfileContent {
    data object Status : SavedOwnerProfileContent
    data object Empty : SavedOwnerProfileContent
    data class Lists(val values: List<SavedCollectionSummary>) : SavedOwnerProfileContent
}

/** Do not expose a previous account's collections during loading or after a failed read. */
internal fun savedOwnerProfileContent(state: SavedCollectionsState): SavedOwnerProfileContent {
    if (!state.signedIn || state.loading || state.failure != null) return SavedOwnerProfileContent.Status
    return if (state.lists.isEmpty()) SavedOwnerProfileContent.Empty else SavedOwnerProfileContent.Lists(state.lists)
}
