package com.coffeepeek.feature.collections.api

/** A screen-owned native presentation of the existing collections library. */
interface CollectionsLibrarySession {
    fun current(): CollectionsLibrarySnapshot
    fun observe(onState: (CollectionsLibrarySnapshot) -> Unit, onEvent: (CollectionsLibraryEvent) -> Unit): CollectionsLibraryObservation
    fun reload()
    fun query(value: String)
    fun openEditor(id: String?)
    fun signIn()
    fun back()
    fun loadCover(id: String, accountVersion: Int, onCover: (ByteArray?, String?) -> Unit): CollectionsLibraryObservation
    fun close()
}

data class CollectionsLibrarySnapshot(
    val accountVersion: Int,
    val signedIn: Boolean,
    val loading: Boolean,
    val refreshing: Boolean,
    val failed: Boolean,
    val query: String,
    val items: List<CollectionsLibraryItem>,
    val matchingItems: List<CollectionsLibraryItem> = items,
)

data class CollectionsLibraryItem(
    val id: String,
    val title: String,
    val isPublic: Boolean,
    val count: Int,
    val imageKey: String,
)

sealed interface CollectionsLibraryEvent {
    data object Back : CollectionsLibraryEvent
    data object SignIn : CollectionsLibraryEvent
    data class Editor(val id: String?) : CollectionsLibraryEvent
}
