package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    CoffeePeekPhotoBackdrop(modifier.fillMaxSize(), photo = {
        CoffeePeekScaffold(Modifier.fillMaxSize(), topBar = { SavedEditorToolbar(state, action, delete) },
            contentWindowInsets = WindowInsets(0)) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                SavedCollectionsStatus(state, action)
                if (state.signedIn && !state.loading) LazyColumn(Modifier.fillMaxSize(), state = lazy,
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp)) {
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
                    }
                }
            }
        }
    }, overlay = { SavedEditorSaveButton(state, action, Modifier.align(Alignment.BottomCenter)) })
}

@Composable
private fun SavedEditorSaveButton(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekGlassButton(stringResource(Res.string.collections_save), { action(SavedCollectionsAction.Save) },
        enabled = state.signedIn && !state.loading && !state.busy && !state.preparingCover && CollectionRules.validTitle(state.title),
        isLoading = state.busy, loadingDescription = stringResource(Res.string.collections_preparing),
        modifier = modifier.fillMaxWidth().coffeePeekViewportBottomPadding().coffeePeekScreenBottomPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(28.dp)))
}

@Composable
private fun SavedEditorToolbar(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, delete: () -> Unit) {
    CoffeePeekTopBar(stringResource(if (state.collection == null) Res.string.collections_new_list else Res.string.collections_edit_list),
        stringResource(Res.string.collections_back), onBack = { action(SavedCollectionsAction.Back) }, actions = {
            if (state.collection != null) SavedEditorMenu(state, action, delete)
        })
}

@Composable
private fun SavedEditorMenu(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit, delete: () -> Unit) {
    val collection = state.collection ?: return
    val shareLink = stringResource(Res.string.collections_share_link)
    val shareImage = stringResource(Res.string.collections_share_photo)
    val deleteList = stringResource(Res.string.collections_delete_list)
    val actions = buildList {
        if (!state.hasEdits()) {
            if (collection.summary.visibility == SavedCollectionVisibility.Public && collection.summary.url != null)
                add(CoffeePeekMenuAction(shareLink, CoffeePeekIcons.Share,
                    onClick = { action(SavedCollectionsAction.ShareLink) }))
            if (collection.places.isNotEmpty())
                add(CoffeePeekMenuAction(shareImage, CoffeePeekIcons.Gallery,
                    onClick = { action(SavedCollectionsAction.Preview) }))
        }
        add(CoffeePeekMenuAction(deleteList, CoffeePeekIcons.Delete,
            destructive = true, onClick = delete))
    }
    CoffeePeekActionMenu(actions, enabled = !state.busy) { open ->
        CoffeePeekPlainIconButton(open, enabled = !state.busy) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.collections_more_actions))
        }
    }
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

@Composable private fun SavedEditorSaveBackdropPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalCoffeePeekScreenInsets provides CoffeePeekScreenInsets(24.dp, true)) {
        CoffeePeekPhotoBackdrop(Modifier.fillMaxWidth().height(180.dp), photo = {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                Box(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)))
            }
        }, overlay = {
            SavedEditorSaveButton(SavedCollectionsState(SavedCollectionsMode.Editor, signedIn = true,
                loading = false, title = "Любимые места"), {}, Modifier.align(Alignment.BottomCenter))
        })
    }
}
@Preview @Composable private fun SavedEditorSaveBackdropLightPreview() = SavedEditorSaveBackdropPreview(false)
@Preview @Composable private fun SavedEditorSaveBackdropDarkPreview() = SavedEditorSaveBackdropPreview(true)
