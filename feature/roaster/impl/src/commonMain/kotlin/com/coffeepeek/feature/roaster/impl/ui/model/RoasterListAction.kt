package com.coffeepeek.feature.roaster.impl.ui.model

internal sealed interface RoasterListAction {
    data object Initialize : RoasterListAction
    data object Refresh : RoasterListAction
    data class QueryChanged(val query: String) : RoasterListAction
    data class ToggleTag(val slug: String) : RoasterListAction
    data object ClearTags : RoasterListAction
    data class FavoritesOnlyChanged(val enabled: Boolean) : RoasterListAction
    /** Discovery's selected-roaster filter, owned by the caller. */
    data class SelectedRoastersChanged(val slugs: Set<String>) : RoasterListAction
    /** A card became visible and needs its details (shop photos, cover fallback). */
    data class LoadDetails(val slug: String) : RoasterListAction
    data class ToggleFavorite(val slug: String) : RoasterListAction
    data class OpenRoaster(val slug: String) : RoasterListAction
    data object DismissMessage : RoasterListAction
}

internal sealed interface RoasterListEvent {
    data class OpenRoaster(val slug: String) : RoasterListEvent
}
