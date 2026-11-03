package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.SharedCollection

internal sealed interface CollectionsEvent {
    data object Back : CollectionsEvent
    data object SignIn : CollectionsEvent
    data object CancelExport : CollectionsEvent
    data class ShareLink(val collection: SharedCollection) : CollectionsEvent
    data class ExportImages(val collection: SharedCollection, val format: CollectionPosterFormat, val generation: Int) : CollectionsEvent
    data class OpenPlace(val place: CollectionPlace) : CollectionsEvent
}
