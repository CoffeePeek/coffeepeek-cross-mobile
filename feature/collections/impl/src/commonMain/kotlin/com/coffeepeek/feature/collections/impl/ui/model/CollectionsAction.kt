package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace

internal sealed interface CollectionsAction {
    data object Retry : CollectionsAction
    data object Back : CollectionsAction
    data object SignIn : CollectionsAction
    data object SelectAll : CollectionsAction
    data object OpenLinks : CollectionsAction
    data object ShareLink : CollectionsAction
    data object ShareImages : CollectionsAction
    data class Toggle(val key: String) : CollectionsAction
    data class Title(val value: String) : CollectionsAction
    data class Create(val defaultTitle: String) : CollectionsAction
    data class Revoke(val id: String) : CollectionsAction
    data class OpenCollection(val id: String) : CollectionsAction
    data class OpenPlace(val place: CollectionPlace) : CollectionsAction
    data class Format(val value: CollectionPosterFormat) : CollectionsAction
    data class ExportFinished(val generation: Int, val collectionId: String, val failed: Boolean) : CollectionsAction
}
