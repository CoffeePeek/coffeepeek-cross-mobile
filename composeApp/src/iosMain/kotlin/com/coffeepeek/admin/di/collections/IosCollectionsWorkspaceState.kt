package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.collections.impl.*

data class IosCollectionsWorkspaceState(
    val mode: String, val accountVersion: Int, val signedIn: Boolean,
    val loading: Boolean, val refreshing: Boolean, val busy: Boolean, val failure: String?,
    val id: String?, val title: String, val description: String, val isPublic: Boolean,
    val hasEdits: Boolean, val canSave: Boolean, val canShare: Boolean, val canPreview: Boolean,
    val preparingCover: Boolean, val hasCover: Boolean, val coverKey: String?, val preview: Boolean, val story: Boolean,
    val featuredId: String?,
    val lists: List<IosCollectionsWorkspaceList>, val places: List<IosCollectionsWorkspacePlace>,
    val query: String = "", val quickTitle: String? = null,
    val membershipsLoaded: Boolean = false, val canCreateQuick: Boolean = false,
    val matchingLists: List<IosCollectionsWorkspaceList> = lists,
)
data class IosCollectionsWorkspaceList(val id: String, val title: String, val isPublic: Boolean,
    val count: Int, val imageKey: String, val selected: Boolean)
data class IosCollectionsWorkspacePlace(val key: String, val title: String, val address: String,
    val roaster: Boolean, val photoUrl: String?, val selected: Boolean, val rating: Double?, val checkIns: Int?)
data class IosCollectionsPoster(val id: String, val title: String, val url: String, val width: Int,
    val height: Int, val pages: List<IosCollectionsPosterPage>)
data class IosCollectionsPosterPage(val cards: List<IosCollectionsPosterCard>)
data class IosCollectionsPosterCard(val place: IosCollectionsWorkspacePlace,
    val left: Float, val top: Float, val width: Float, val height: Float)

internal fun CollectionsWorkspaceState.iosState() = IosCollectionsWorkspaceState(
    mode, accountVersion, signedIn, loading, refreshing, busy, failure, id, title, description, isPublic,
    hasEdits, canSave, canShare, canPreview, preparingCover, hasCover, coverKey, preview, story, featuredId,
    lists.map { it.iosList() },
    places.map { it.iosPlace() },
    query, quickTitle, membershipsLoaded, canCreateQuick,
    matchingLists.map { it.iosList() },
)
private fun CollectionsWorkspaceList.iosList() = IosCollectionsWorkspaceList(id, title, isPublic, count, imageKey, selected)
internal fun CollectionsWorkspacePlace.iosPlace() = IosCollectionsWorkspacePlace(
    key, title, address, roaster, photoUrl, selected, rating, checkIns,
)
internal fun CollectionsPoster.iosPoster() = IosCollectionsPoster(id, title, url, width, height,
    pages.map { page -> IosCollectionsPosterPage(page.cards.map {
        IosCollectionsPosterCard(it.place.iosPlace(), it.left, it.top, it.width, it.height)
    }) })

