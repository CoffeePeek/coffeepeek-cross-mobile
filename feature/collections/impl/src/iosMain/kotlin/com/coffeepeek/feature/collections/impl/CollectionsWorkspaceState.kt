package com.coffeepeek.feature.collections.impl

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.*

enum class CollectionsWorkspaceMode { Editor, Owner, Preference, Featured, Public, Snapshot }

data class CollectionsWorkspaceState(
    val mode: String, val accountVersion: Int, val signedIn: Boolean,
    val loading: Boolean, val refreshing: Boolean, val busy: Boolean, val failure: String?,
    val id: String?, val title: String, val description: String, val isPublic: Boolean,
    val hasEdits: Boolean, val canSave: Boolean, val canShare: Boolean, val canPreview: Boolean,
    val preparingCover: Boolean, val hasCover: Boolean, val coverKey: String?, val preview: Boolean, val story: Boolean,
    val featuredId: String?,
    val lists: List<CollectionsWorkspaceList>, val places: List<CollectionsWorkspacePlace>,
)

data class CollectionsWorkspaceList(val id: String, val title: String, val isPublic: Boolean,
    val count: Int, val imageKey: String, val selected: Boolean)

data class CollectionsWorkspacePlace(val key: String, val title: String, val address: String,
    val roaster: Boolean, val photoUrl: String?, val selected: Boolean, val rating: Double?, val checkIns: Int?)

data class CollectionsPoster(val id: String, val title: String, val url: String, val width: Int,
    val height: Int, val pages: List<CollectionsPosterPage>)
data class CollectionsPosterPage(val cards: List<CollectionsPosterCard>)
data class CollectionsPosterCard(val place: CollectionsWorkspacePlace,
    val left: Float, val top: Float, val width: Float, val height: Float)

internal fun CollectionPlace.nativePlace(selected: Boolean = true) = CollectionsWorkspacePlace(
    key, title, listOfNotNull(city, address).filter(String::isNotBlank).joinToString(" · "),
    kind == CollectionPlaceKind.Roaster, photoUrl, selected, rating, checkInCount,
)

internal fun SharedCollection.nativePoster(format: CollectionPosterFormat): CollectionsPoster {
    val layout = collectionPosterLayout(places, format, contentTop = 228f)
    return CollectionsPoster(id, title, url, format.width, format.height, layout.pages.map { page ->
        CollectionsPosterPage(page.map { CollectionsPosterCard(it.place.nativePlace(), it.left, it.top, it.width, it.height) })
    })
}
