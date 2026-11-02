package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
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
    callbacks: CollectionsCallbacks, modifier: Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val currentCallbacks by rememberUpdatedState(callbacks)
    val labels by rememberUpdatedState(collectionPosterLabels(state.collection, state.format))
    val typography by rememberUpdatedState(MaterialTheme.typography.bodyLarge)
    val measurer = rememberTextMeasurer(cacheSize = 0)
    BackHandler { vm.onAction(CollectionsAction.Back) }
    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            handleCollectionEvent(event, vm, repository, decodePhoto, currentCallbacks, labels, measurer, typography)
        }
    }
    CollectionsScreenContent(state, vm::onAction, modifier) { collection, format, page ->
        CollectionPoster(collection, format, page, repository, decodePhoto)
    }
}

private suspend fun handleCollectionEvent(event: CollectionsEvent, vm: CollectionsViewModel, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    callbacks: CollectionsCallbacks, labels: CollectionPosterLabels, measurer: TextMeasurer, typography: TextStyle) {
    when (event) {
        CollectionsEvent.Back -> callbacks.back()
        CollectionsEvent.SignIn -> callbacks.signIn()
        CollectionsEvent.CancelExport -> Unit
        is CollectionsEvent.OpenPlace -> if (event.place.kind == CollectionPlaceKind.CoffeeShop) callbacks.openShop(event.place.slug)
            else callbacks.openRoaster(event.place.slug)
        is CollectionsEvent.ShareLink -> if (vm.state.value.collection?.id == event.collection.id)
            callbacks.shareText("${event.collection.title}\n${event.collection.url}")
        is CollectionsEvent.ExportImages -> exportAndShare(event, vm, repository, decodePhoto, callbacks, labels, measurer, typography)
    }
}

private suspend fun exportAndShare(event: CollectionsEvent.ExportImages, vm: CollectionsViewModel, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    callbacks: CollectionsCallbacks, labels: CollectionPosterLabels, measurer: TextMeasurer, typography: TextStyle) {
    var failed = false
    try {
        val images = exportPosters(event.collection, event.format, repository, decodePhoto, labels, measurer, typography)
        if (vm.isCurrentExport(event.generation, event.collection.id)) {
            callbacks.shareImages(images, "${event.collection.title}\n${event.collection.url}")
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed = true }
    finally {
        vm.onAction(CollectionsAction.ExportFinished(event.generation, event.collection.id, failed))
    }
}
