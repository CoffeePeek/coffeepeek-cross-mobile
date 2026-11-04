package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SharedCollectionMode
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.CollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.component.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest

internal class CollectionsCallbacks(val back: () -> Unit, val signIn: () -> Unit,
    val openShop: (String) -> Unit, val openRoaster: (String) -> Unit, val shareText: (String) -> Unit,
    val shareImages: (List<ByteArray>, String) -> Unit)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun CollectionsScreen(vm: CollectionsViewModel, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    callbacks: CollectionsCallbacks, modifier: Modifier, savedPublic: @Composable (String) -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.publicView && state.collection?.mode == SharedCollectionMode.List) {
        savedPublic(checkNotNull(state.collection).id)
        return
    }
    val currentCallbacks by rememberUpdatedState(callbacks)
    val resources by rememberUpdatedState(collectionPosterResources(state.collection, state.format))
    BackHandler { vm.onAction(CollectionsAction.Back) }
    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            handleCollectionEvent(event, vm, repository, decodePhoto, currentCallbacks, resources)
        }
    }
    CollectionsScreenContent(state, vm::onAction, modifier) { collection, format, page ->
        CollectionPoster(collection, format, page, repository, decodePhoto)
    }
}

private suspend fun handleCollectionEvent(event: CollectionsEvent, vm: CollectionsViewModel, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    callbacks: CollectionsCallbacks, resources: CollectionPosterResources) {
    when (event) {
        CollectionsEvent.Back -> callbacks.back()
        CollectionsEvent.SignIn -> callbacks.signIn()
        CollectionsEvent.CancelExport -> Unit
        is CollectionsEvent.OpenPlace -> if (event.place.kind == CollectionPlaceKind.CoffeeShop) callbacks.openShop(event.place.slug)
            else callbacks.openRoaster(event.place.slug)
        is CollectionsEvent.ShareLink -> if (vm.state.value.collection?.id == event.collection.id)
            callbacks.shareText("${event.collection.title}\n${event.collection.url}")
        is CollectionsEvent.ExportImages -> exportAndShare(event, vm, repository, decodePhoto, callbacks, resources)
    }
}

private suspend fun exportAndShare(event: CollectionsEvent.ExportImages, vm: CollectionsViewModel, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    callbacks: CollectionsCallbacks, resources: CollectionPosterResources) {
    var failed = false
    try {
        val images = exportPosters(event.collection, event.format, repository, decodePhoto, resources)
        if (vm.isCurrentExport(event.generation, event.collection.id)) {
            callbacks.shareImages(images, "${event.collection.title}\n${event.collection.url}")
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed = true }
    finally {
        vm.onAction(CollectionsAction.ExportFinished(event.generation, event.collection.id, failed))
    }
}
