package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.feature.collections.api.*
import com.coffeepeek.feature.collections.domain.model.CollectionCoverInput
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.compose.component.SavedCollectionCoverImage
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedEditorRuntime(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    saved: SavedCollectionsRepository, decode: CollectionPhotoDecoder, picker: CollectionCoverPicker, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var pickerVersion by remember { mutableIntStateOf(state.accountVersion) }
    LaunchedEffect(state.accountVersion) {
        picking = false
        deleting = false
    }
    SavedCollectionEditor(state, action, {
        pickerVersion = state.accountVersion
        picking = true
    }, { deleting = true }, modifier) { m ->
        SavedCollectionCoverImage(state.collection?.summary?.takeUnless { state.removeCover }, state.coverInput, saved, decode, m)
    }
    key(state.accountVersion) {
        val version = state.accountVersion
        picker(picking && pickerVersion == version, { picking = false }, { action(SavedCollectionsAction.CoverPreparing(it, version)) }, { picked ->
            action(SavedCollectionsAction.CoverPicked(CollectionCoverInput(picked.bytes, picked.mimeType, picked.fileName), version))
        })
    }
    if (deleting) CoffeePeekDestructiveConfirmationSheet(stringResource(Res.string.collections_delete_title),
        stringResource(Res.string.collections_delete_message), stringResource(Res.string.collections_delete_list),
        stringResource(Res.string.collections_cancel), {
            deleting = false
            action(SavedCollectionsAction.Delete)
        }, { deleting = false })
}
