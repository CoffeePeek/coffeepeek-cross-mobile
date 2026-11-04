package com.coffeepeek.feature.collections.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.collections.api.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.CollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.SavedCollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.CollectionsCallbacks
import com.coffeepeek.feature.collections.impl.ui.compose.CollectionsScreen
import com.coffeepeek.feature.collections.impl.ui.compose.SavedCollectionsScreen
import com.coffeepeek.feature.collections.impl.ui.model.SavedCollectionsMode
import kotlin.uuid.Uuid

fun createCollectionsEntry(repository: CollectionsRepository, saved: SavedCollectionsRepository, account: CollectionsAccount,
    decodePhoto: CollectionPhotoDecoder, coverPicker: CollectionCoverPicker): CollectionsEntry =
    CollectionsEntryImpl(repository, saved, account, decodePhoto, coverPicker)

private class CollectionsEntryImpl(private val repository: CollectionsRepository, private val saved: SavedCollectionsRepository,
    private val account: CollectionsAccount, private val decodePhoto: CollectionPhotoDecoder, private val coverPicker: CollectionCoverPicker) : CollectionsEntry {
    private val instance = Uuid.random().toString()


    @Composable
    override fun Composer(onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, onShareText: (String) -> Unit,
        onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        Content(null, CollectionsCallbacks(onBack, onSignIn, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier)
    }

    @Composable
    override fun Public(id: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        Content(id, CollectionsCallbacks(onBack, {}, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier)
    }

    @Composable
    override fun Library(onBack: () -> Unit, onSignIn: () -> Unit, onOpenEditor: (String?) -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Library, callbacks = callbacks(onBack, onSignIn), editor = onOpenEditor, modifier = modifier)
    }

    @Composable
    override fun Editor(id: String?, onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Editor, id = id,
            callbacks = CollectionsCallbacks(onBack, onSignIn, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier = modifier)
    }

    @Composable
    override fun Picker(target: CollectionTarget, onDismiss: () -> Unit, onSignIn: () -> Unit, modifier: Modifier) {
        val kind = if (target.kind == CollectionTargetKind.CoffeeShop) CollectionPlaceKind.CoffeeShop else CollectionPlaceKind.Roaster
        SavedContent(SavedCollectionsMode.Picker, target = SavedCollectionTarget(kind, target.slug),
            callbacks = callbacks(onDismiss, onSignIn), modifier = modifier)
    }

    @Composable
    override fun ProfilePreference(onSignIn: () -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Preference, callbacks = callbacks({}, onSignIn), modifier = modifier)
    }

    @Composable
    override fun OwnerProfile(onOpenCollection: (String) -> Unit, onSignIn: () -> Unit, fallback: @Composable () -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Preference, callbacks = callbacks({}, onSignIn), openCollection = onOpenCollection,
            ownerProfileFallback = fallback, modifier = modifier)
    }

    @Composable
    override fun Featured(profileSlug: String, onOpenCollection: (String) -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Featured, profileSlug = profileSlug, openCollection = onOpenCollection, modifier = modifier)
    }

    @Composable
    override fun SavedPublic(id: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        SavedContent(SavedCollectionsMode.Public, id = id,
            callbacks = CollectionsCallbacks(onBack, {}, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier = modifier)
    }

    @Composable
    private fun Content(id: String?, callbacks: CollectionsCallbacks, modifier: Modifier) {
        val model = viewModel(key = "collections:$instance:${id ?: "compose"}") { CollectionsViewModel(repository, account, id) }
        CollectionsScreen(model, repository, decodePhoto, callbacks, modifier) { listId ->
            SavedContent(SavedCollectionsMode.Public, id = listId, callbacks = callbacks, modifier = modifier)
        }
    }

    @Composable
    private fun SavedContent(mode: SavedCollectionsMode, id: String? = null, target: SavedCollectionTarget? = null,
        profileSlug: String? = null, callbacks: CollectionsCallbacks = callbacks(), editor: (String?) -> Unit = {},
        openCollection: (String) -> Unit = {}, ownerProfileFallback: (@Composable () -> Unit)? = null, modifier: Modifier) {
        val model = viewModel(key = "saved:$instance:$mode:$id:$target:$profileSlug:${ownerProfileFallback != null}") {
            SavedCollectionsViewModel(saved, repository, account, mode, id, target, profileSlug)
        }
        SavedCollectionsScreen(model, saved, repository, decodePhoto, coverPicker, callbacks, editor, openCollection, modifier, ownerProfileFallback)
    }

    private fun callbacks(back: () -> Unit = {}, signIn: () -> Unit = {}) = CollectionsCallbacks(back, signIn, {}, {}, {}, { _, _ -> })
}
