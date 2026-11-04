package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility

internal sealed interface SavedOwnerProfileContent {
    data object Status : SavedOwnerProfileContent
    data object Favorites : SavedOwnerProfileContent
    data class Selected(val value: SavedCollectionSummary) : SavedOwnerProfileContent
}

/** A failed or incomplete preference read must never fall back to unrelated favorites. */
internal fun savedOwnerProfileContent(state: SavedCollectionsState): SavedOwnerProfileContent {
    if (!state.signedIn || state.loading || state.failure != null) return SavedOwnerProfileContent.Status
    val id = state.featuredId ?: return SavedOwnerProfileContent.Favorites
    val selected = state.lists.firstOrNull { it.id == id && it.visibility == SavedCollectionVisibility.Public }
    return selected?.let { SavedOwnerProfileContent.Selected(it) } ?: SavedOwnerProfileContent.Status
}
