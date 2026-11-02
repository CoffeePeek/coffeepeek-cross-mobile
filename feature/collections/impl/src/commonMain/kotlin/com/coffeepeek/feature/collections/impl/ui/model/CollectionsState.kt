package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.CollectionSummary
import com.coffeepeek.feature.collections.domain.model.SharedCollection

internal enum class CollectionsPage { Selection, Preview, Links }
internal enum class CollectionsFailure { Load, Create, Revoke, Export, SelectionLimit }
internal enum class CollectionPosterFormat(val width: Int, val height: Int, val perPage: Int) {
    Chat(1080, 1350, 4), Story(1080, 1920, 6)
}

internal data class CollectionsState(
    val signedIn: Boolean = false,
    val publicView: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val page: CollectionsPage = CollectionsPage.Selection,
    val title: String = "",
    val places: List<CollectionPlace> = emptyList(),
    val selected: Set<String> = emptySet(),
    val collection: SharedCollection? = null,
    val links: List<CollectionSummary> = emptyList(),
    val failure: CollectionsFailure? = null,
    val format: CollectionPosterFormat = CollectionPosterFormat.Chat,
)
