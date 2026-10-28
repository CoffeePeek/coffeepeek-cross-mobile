package com.coffeepeek.feature.roaster.impl.ui.model

import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.model.RoasterTag

internal data class RoasterListItem(val summary: RoasterSummary, val details: RoasterDetails? = null) {
    val slug: String? get() = summary.slug
    /** Favorite identity: the public slug (the legacy catalogue id equals it). */
    val favoriteKey: String get() = summary.address?.slug.orEmpty()
}

internal sealed interface RoasterMessage {
    data object FavoritesUnavailable : RoasterMessage
    data object FavoriteChangeFailed : RoasterMessage
    /** Text reported by the favorites store itself. */
    data class FromStore(val text: String) : RoasterMessage
}

internal data class RoasterListState(
    val items: List<RoasterListItem> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val selectedTags: Set<String> = emptySet(),
    val selectedRoasters: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
    val favoriteKeys: Set<String> = emptySet(),
    val message: RoasterMessage? = null,
) {
    fun hasResults(query: String, selectedRoasters: Set<String>, favoritesOnly: Boolean): Boolean? =
        if (isLoading || loadFailed) null
        else copy(query = query, selectedRoasters = selectedRoasters, favoritesOnly = favoritesOnly).visibleItems.isNotEmpty()

    val availableTags: List<RoasterTag>
        get() = items.flatMap { it.summary.tags }.distinctBy { it.slug }.sortedBy { it.sortOrder }

    val visibleItems: List<RoasterListItem>
        get() = items.filter { item ->
            item.summary.name.contains(query.trim(), ignoreCase = true) &&
                (selectedRoasters.isEmpty() || item.slug in selectedRoasters) &&
                (!favoritesOnly || item.favoriteKey in favoriteKeys) &&
                selectedTags.all { tag -> item.summary.tags.any { it.slug == tag } }
        }
}
