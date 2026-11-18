package com.coffeepeek.feature.collections.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.ui.CollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/** Old snapshot links remain readable; mutable list links use the saved-public destination. */
class PublicCollectionSession(
    id: String, private val repository: CollectionsRepository, account: CollectionsAccount,
    private val onBack: () -> Unit, private val onList: (String) -> Unit,
    private val onShop: (String) -> Unit, private val onRoaster: (String) -> Unit,
    private val onShare: (String) -> Unit,
) {
    private val model = CollectionsViewModel(repository, account, id)
    private val store = ViewModelStore().apply { put("public-snapshot", model) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var closed = false
    private var redirected: String? = null
    private var export: ((CollectionsPoster, Int) -> Unit)? = null

    init {
        scope.launch { model.events.collect(::handleEvent) }
        scope.launch { model.state.collect { state ->
            state.collection?.takeIf { it.mode == SharedCollectionMode.List && it.id != redirected }?.let {
                redirected = it.id
                onList(it.id)
            }
        } }
    }

    fun current(): CollectionsWorkspaceState {
        val state = model.state.value
        val collection = state.collection?.takeIf { it.mode == SharedCollectionMode.Snapshot }
        return CollectionsWorkspaceState(mode = "Snapshot", accountVersion = 0, signedIn = false,
            loading = state.loading || redirected != null, refreshing = false, busy = state.busy, failure = state.failure?.name,
            id = collection?.id, title = collection?.title.orEmpty(), description = "", isPublic = true,
            hasEdits = false, canSave = false, canShare = collection != null, canPreview = false,
            preparingCover = false, hasCover = false, coverKey = null, preview = true, story = state.format == CollectionPosterFormat.Story,
            featuredId = null, lists = emptyList(),
            places = collection?.places.orEmpty().map { it.nativePlace() })
    }

    fun observe(onState: () -> Unit, onExport: (CollectionsPoster, Int) -> Unit): CollectionsLibraryObservation {
        export = onExport
        val job = scope.launch { model.state.collect { onState() } }
        return CollectionsLibraryObservation { job.cancel(); if (export === onExport) export = null }
    }

    fun reload() = dispatch(CollectionsAction.Retry)
    fun back() = dispatch(CollectionsAction.Back)
    fun shareLink() = dispatch(CollectionsAction.ShareLink)
    fun shareImages() = dispatch(CollectionsAction.ShareImages)
    fun format(story: Boolean) = dispatch(CollectionsAction.Format(if (story) CollectionPosterFormat.Story else CollectionPosterFormat.Chat))
    fun poster(): CollectionsPoster? = model.state.value.let { it.collection?.nativePoster(it.format) }
    fun isCurrentExport(epoch: Int, id: String): Boolean = !closed && model.isCurrentExport(epoch, id)
    fun finishExport(epoch: Int, id: String, failed: Boolean) = dispatch(CollectionsAction.ExportFinished(epoch, id, failed))

    fun openPlace(key: String) {
        model.state.value.collection?.places?.firstOrNull { it.key == key }?.let { dispatch(CollectionsAction.OpenPlace(it)) }
    }

    fun loadPhoto(url: String, receive: (ByteArray?) -> Unit): CollectionsLibraryObservation {
        val job = scope.launch {
            val bytes = repository.photo(url)
            try { if (!closed) receive(bytes) } finally { bytes?.fill(0) }
        }
        return CollectionsLibraryObservation { job.cancel() }
    }

    fun close() {
        if (closed) return
        closed = true
        export = null
        scope.cancel()
        store.clear()
    }

    private fun dispatch(action: CollectionsAction) { if (!closed) model.onAction(action) }

    private suspend fun handleEvent(event: CollectionsEvent) {
        when (event) {
            CollectionsEvent.Back -> onBack()
            is CollectionsEvent.OpenPlace -> if (event.place.kind == CollectionPlaceKind.CoffeeShop) onShop(event.place.slug) else onRoaster(event.place.slug)
            is CollectionsEvent.ShareLink -> onShare("${event.collection.title}\n${event.collection.url}")
            is CollectionsEvent.ExportImages -> export?.invoke(event.collection.nativePoster(event.format), event.generation)
                ?: finishExport(event.generation, event.collection.id, true)
            else -> Unit
        }
    }
}
