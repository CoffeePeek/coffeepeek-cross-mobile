package com.coffeepeek.feature.collections.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollection
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.SavedCollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.CollectionsCallbacks
import com.coffeepeek.feature.collections.impl.ui.compose.SavedCollectionPoster
import com.coffeepeek.feature.collections.impl.ui.compose.component.collectionPosterResources
import com.coffeepeek.feature.collections.impl.ui.compose.shareSavedImages
import com.coffeepeek.feature.collections.impl.ui.model.SavedCollectionsAction
import com.coffeepeek.feature.collections.impl.ui.model.SavedCollectionsEvent
import com.coffeepeek.feature.collections.impl.ui.model.SavedCollectionsMode
import com.coffeepeek.feature.collections.impl.ui.model.toSharedPoster
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SavedPublicCollectionSession(
    id: String,
    private val saved: SavedCollectionsRepository,
    private val snapshots: CollectionsRepository,
    account: CollectionsAccount,
    back: () -> Unit,
    openShop: (String) -> Unit,
    openRoaster: (String) -> Unit,
    shareText: (String) -> Unit,
    shareImages: (List<ByteArray>, String) -> Unit,
) {
    private val model = SavedCollectionsViewModel(saved, snapshots, account, SavedCollectionsMode.Public, id)
    private val store = ViewModelStore().apply { put("saved-public", model) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val callbacks = CollectionsCallbacks(back, {}, openShop, openRoaster, shareText, shareImages)
    private var exportPoster: (suspend (SavedCollectionsEvent.Export) -> Unit)? = null
    private var closed = false

    init {
        scope.launch { model.events.collectLatest(::handleEvent) }
    }

    fun current(): SavedPublicCollectionSnapshot {
        val state = model.state.value
        val collection = visibleCollection()
        return SavedPublicCollectionSnapshot(
            loading = state.loading, failed = state.failure != null, busy = state.busy,
            preview = state.preview, title = collection?.summary?.title, description = collection?.summary?.description.orEmpty(),
            coverKey = collection?.coverKey(), canShare = collection?.summary?.url != null,
            places = collection?.places.orEmpty().map {
                SavedPublicPlace(it.key, it.title, listOfNotNull(it.city, it.address).filter(String::isNotBlank).joinToString(" · "),
                    it.kind == CollectionPlaceKind.Roaster, it.photoUrl)
            },
        )
    }

    fun observe(onState: () -> Unit): CollectionsLibraryObservation {
        val job = scope.launch { model.state.collect { onState() } }
        return CollectionsLibraryObservation { job.cancel() }
    }

    fun back() = dispatch(SavedCollectionsAction.Back)
    fun reload() = dispatch(SavedCollectionsAction.Reload)
    fun preview() { if (visibleCollection()?.places?.isNotEmpty() == true) dispatch(SavedCollectionsAction.Preview) }
    fun shareLink() { if (visibleCollection()?.summary?.url != null) dispatch(SavedCollectionsAction.ShareLink) }

    fun openPlace(key: String) {
        visibleCollection()?.places?.firstOrNull { it.key == key }?.let { dispatch(SavedCollectionsAction.OpenPlace(it)) }
    }

    fun loadCover(key: String, onCover: (ByteArray?, String?) -> Unit): CollectionsLibraryObservation {
        val job = scope.launch {
            val collection = visibleCollection()?.takeIf { it.coverKey() == key } ?: return@launch
            deliverCover(collection, key, onCover)
        }
        return CollectionsLibraryObservation { job.cancel() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Poster(decode: CollectionPhotoDecoder) {
        val state by model.state.collectAsStateWithLifecycle()
        if (!state.preview) return
        val resources = collectionPosterResources(state.collection?.toSharedPoster(), state.format)
        val export by rememberUpdatedState<suspend (SavedCollectionsEvent.Export) -> Unit> {
            shareSavedImages(it, model, snapshots, decode, callbacks, resources)
        }
        DisposableEffect(this) {
            exportPoster = { export(it) }
            onDispose { exportPoster = null }
        }
        BackHandler { back() }
        SavedCollectionPoster(state, ::dispatch, snapshots, decode, Modifier)
    }

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        exportPoster = null
        store.clear()
    }

    private fun dispatch(action: SavedCollectionsAction) {
        if (!closed) model.onAction(action)
    }

    private suspend fun handleEvent(event: SavedCollectionsEvent) {
        when (event) {
            SavedCollectionsEvent.Back -> callbacks.back()
            is SavedCollectionsEvent.Place -> if (event.value.kind == CollectionPlaceKind.CoffeeShop)
                callbacks.openShop(event.value.slug) else callbacks.openRoaster(event.value.slug)
            is SavedCollectionsEvent.Share -> callbacks.shareText(event.value)
            is SavedCollectionsEvent.Export -> exportPoster?.invoke(event)
                ?: dispatch(SavedCollectionsAction.ExportFinished(event.epoch, failed = true))
            else -> Unit
        }
    }

    private suspend fun deliverCover(collection: SavedCollection, key: String, onCover: (ByteArray?, String?) -> Unit) {
        var bytes: ByteArray? = null
        try {
            val cover = collection.summary.cover
            val publicShopPhoto = if (cover == null) collection.firstShopPhoto() else null
            if (cover != null && !cover.authenticated) bytes = saved.cover(collection.summary.id, cover).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (visibleCollection()?.coverKey() == key) onCover(bytes, publicShopPhoto)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (visibleCollection()?.coverKey() == key) onCover(null, null)
        } finally {
            bytes?.fill(0)
        }
    }

    private fun visibleCollection(): SavedCollection? =
        model.state.value.collection?.takeIf { !closed && it.summary.visibility == SavedCollectionVisibility.Public }
}

private fun SavedCollection.coverKey(): String =
    "${summary.id}:${summary.revision}:${summary.cover}:${firstShopPhoto()}"

private fun SavedCollection.firstShopPhoto(): String? = places.firstOrNull {
    it.kind == CollectionPlaceKind.CoffeeShop
}?.photoUrl?.takeIf(String::isNotBlank)

data class SavedPublicCollectionSnapshot(
    val loading: Boolean,
    val failed: Boolean,
    val busy: Boolean,
    val preview: Boolean,
    val title: String?,
    val description: String,
    val coverKey: String?,
    val canShare: Boolean,
    val places: List<SavedPublicPlace>,
)

data class SavedPublicPlace(
    val key: String,
    val title: String,
    val address: String,
    val roaster: Boolean,
    val photoUrl: String?,
)
