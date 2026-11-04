package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.CollectionReorderablePlaceRow
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionEditor(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    pick: () -> Unit, delete: () -> Unit, modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    val lazy = rememberLazyListState()
    var dragging by remember { mutableStateOf<CollectionPlaceKind?>(null) }
    val reorder = rememberReorderableLazyListState(lazy) { from, to ->
        action(SavedCollectionsAction.MovePlace(from.key as String, to.key as String, from.index < to.index))
    }
    CoffeePeekScaffold(modifier, topBar = { SavedEditorToolbar(state, action) }, bottomBar = {
        CoffeePeekButton(stringResource(Res.string.collections_save), { action(SavedCollectionsAction.Save) },
            enabled = state.signedIn && !state.loading && !state.busy && !state.preparingCover && CollectionRules.validTitle(state.title),
            modifier = Modifier.fillMaxWidth().padding(16.dp))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SavedCollectionsStatus(state, action)
            if (state.signedIn && !state.loading) LazyColumn(Modifier.fillMaxSize(), state = lazy,
                contentPadding = PaddingValues(16.dp)) {
                item { SavedCollectionMetadataForm(state, action, pick, image) }
                if (state.collection != null) {
                    item { Text(stringResource(Res.string.collections_manage_places), Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium) }
                    CollectionPlaceKind.entries.forEach { kind ->
                        val places = state.places.filter { it.kind == kind }
                        itemsIndexed(places, key = { _, place -> place.key }) { index, place ->
                            CollectionReorderablePlaceRow(places, index, place.key in state.selectedPlaces, !state.busy,
                                dragging == null || dragging == kind, reorder, { dragging = it }) { editorPlaceAction(it, action) }
                        }
                    }
                    if (state.places.isEmpty()) item { Text(stringResource(Res.string.collections_saved_empty)) }
                    item { CoffeePeekTextButton(delete, enabled = !state.busy) {
                        Text(stringResource(Res.string.collections_delete_list), color = MaterialTheme.colorScheme.error)
                    } }
                }
            }
        }
    }
}

@Composable
private fun SavedEditorToolbar(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    CoffeePeekTopBar(stringResource(if (state.collection == null) Res.string.collections_new_list else Res.string.collections_edit_list),
        stringResource(Res.string.collections_back), onBack = { action(SavedCollectionsAction.Back) }, actions = {
            if (state.collection?.summary?.visibility == SavedCollectionVisibility.Public && state.collection?.summary?.url != null)
                CoffeePeekPlainIconButton({ action(SavedCollectionsAction.ShareLink) }, enabled = !state.busy && !state.hasEdits()) {
                    Icon(CoffeePeekIcons.Share, stringResource(Res.string.collections_share_link))
                }
            if (state.collection?.places?.isNotEmpty() == true) CoffeePeekPlainIconButton({ action(SavedCollectionsAction.Preview) },
                enabled = !state.busy && !state.hasEdits()) { Icon(CoffeePeekIcons.Gallery, stringResource(Res.string.collections_picture)) }
        })
}

private fun editorPlaceAction(value: CollectionsAction, action: (SavedCollectionsAction) -> Unit) {
    when (value) {
        is CollectionsAction.Toggle -> action(SavedCollectionsAction.TogglePlace(value.key))
        is CollectionsAction.Move -> action(SavedCollectionsAction.MovePlace(value.key, value.targetKey, value.after))
        else -> Unit
    }
}

@Composable private fun SavedEditorSample(dark: Boolean, existing: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val shop = CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня")
    val collection = SavedCollection(SavedCollectionSummary("sample", "Мои места", itemCount = 1), listOf(shop))
    SavedCollectionEditor(SavedCollectionsState(SavedCollectionsMode.Editor, signedIn = true, loading = false,
        title = if (existing) "Мои места" else "", collection = collection.takeIf { existing },
        places = listOf(shop).takeIf { existing }.orEmpty(), selectedPlaces = setOf(shop.key).takeIf { existing }.orEmpty()), {}, {}, {})
}
@Preview @Composable private fun SavedEditorLightPreview() = SavedEditorSample(false)
@Preview @Composable private fun SavedEditorDarkPreview() = SavedEditorSample(true)
@Preview @Composable private fun SavedExistingEditorLightPreview() = SavedEditorSample(false, existing = true)
@Preview @Composable private fun SavedExistingEditorDarkPreview() = SavedEditorSample(true, existing = true)
