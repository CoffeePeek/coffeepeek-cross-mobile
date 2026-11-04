package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.coffeepeek.feature.collections.api.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.ui.SavedCollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.component.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun SavedCollectionsScreen(model: SavedCollectionsViewModel, saved: SavedCollectionsRepository,
    snapshots: CollectionsRepository, decode: CollectionPhotoDecoder, picker: CollectionCoverPicker,
    callbacks: CollectionsCallbacks, openEditor: (String?) -> Unit, openCollection: (String) -> Unit, modifier: Modifier,
    profilePreview: Boolean = false) {
    val state by model.state.collectAsStateWithLifecycle()
    val actions = model::onAction
    val currentCallbacks by rememberUpdatedState(callbacks)
    val currentEditor by rememberUpdatedState(openEditor)
    val resources by rememberUpdatedState(collectionPosterResources(state.collection?.toSharedPoster(), state.format))
    if (state.mode in setOf(SavedCollectionsMode.Library, SavedCollectionsMode.Preference, SavedCollectionsMode.Featured))
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { actions(SavedCollectionsAction.Reload) }
    if (state.mode in setOf(SavedCollectionsMode.Library, SavedCollectionsMode.Editor, SavedCollectionsMode.Public))
        BackHandler { actions(SavedCollectionsAction.Back) }
    LaunchedEffect(model) {
        model.events.collectLatest { event ->
            handleSavedEvent(event, model, snapshots, decode, currentCallbacks, currentEditor, resources)
        }
    }
    val image: @Composable (SavedCollectionSummary?, Modifier) -> Unit = { summary, m -> SavedCollectionCoverImage(summary, null, saved, decode, m) }
    if (state.preview) SavedCollectionPoster(state, actions, snapshots, decode, modifier)
    else if (profilePreview) SavedCollectionOwnerProfile(state, actions, openCollection, modifier) { value, m -> image(value, m) }
    else when (state.mode) {
        SavedCollectionsMode.Library -> SavedCollectionsLibrary(state, actions, modifier) { value, m -> image(value, m) }
        SavedCollectionsMode.Editor -> SavedEditorRuntime(state, actions, saved, decode, picker, modifier)
        SavedCollectionsMode.Picker -> SavedCollectionsPicker(state, actions, modifier) { value, m -> image(value, m) }
        SavedCollectionsMode.Preference -> SavedCollectionProfilePreference(state, actions, modifier)
        SavedCollectionsMode.Public -> SavedCollectionPublicContent(state, actions, modifier) { image(state.collection?.summary, it) }
        SavedCollectionsMode.Featured -> SavedCollectionFeatured(state.collection?.summary, openCollection, modifier) { image(state.collection?.summary, it) }
    }
}

private suspend fun handleSavedEvent(event: SavedCollectionsEvent, model: SavedCollectionsViewModel,
    snapshots: CollectionsRepository, decode: CollectionPhotoDecoder, callbacks: CollectionsCallbacks,
    editor: (String?) -> Unit, resources: CollectionPosterResources) {
    when (event) {
        SavedCollectionsEvent.Back -> callbacks.back()
        SavedCollectionsEvent.SignIn -> callbacks.signIn()
        SavedCollectionsEvent.CancelExport -> Unit
        is SavedCollectionsEvent.Editor -> editor(event.id)
        is SavedCollectionsEvent.Place -> if (event.value.kind == CollectionPlaceKind.CoffeeShop) callbacks.openShop(event.value.slug)
            else callbacks.openRoaster(event.value.slug)
        is SavedCollectionsEvent.Share -> callbacks.shareText(event.value)
        is SavedCollectionsEvent.Export -> shareSavedImages(event, model, snapshots, decode, callbacks, resources)
    }
}

private suspend fun shareSavedImages(event: SavedCollectionsEvent.Export, model: SavedCollectionsViewModel,
    snapshots: CollectionsRepository, decode: CollectionPhotoDecoder, callbacks: CollectionsCallbacks, resources: CollectionPosterResources) {
    var failed = false
    try {
        val images = exportPosters(event.value, event.format, snapshots, decode, resources)
        if (model.isCurrentExport(event.epoch, event.value.id)) callbacks.shareImages(images,
            listOf(event.value.title, event.value.url).filter(String::isNotBlank).joinToString("\n"))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed = true }
    finally { model.onAction(SavedCollectionsAction.ExportFinished(event.epoch, failed)) }
}

@Composable
private fun SavedCollectionPoster(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    snapshots: CollectionsRepository, decode: CollectionPhotoDecoder, modifier: Modifier) {
    val collection = state.collection?.toSharedPoster() ?: return
    CollectionsScreenContent(CollectionsState(publicView = true, loading = false, busy = state.busy,
        page = CollectionsPage.Preview, collection = collection, format = state.format,
        failure = if (state.failure == SavedCollectionsFailure.Save) CollectionsFailure.Export else null), { value ->
        when (value) {
            CollectionsAction.Back -> action(SavedCollectionsAction.Back)
            CollectionsAction.ShareLink -> action(SavedCollectionsAction.ShareLink)
            CollectionsAction.ShareImages -> action(SavedCollectionsAction.ShareImages)
            is CollectionsAction.Format -> action(SavedCollectionsAction.Format(value.value))
            else -> Unit
        }
    }, modifier) { value, format, page -> CollectionPoster(value, format, page, snapshots, decode) }
}
