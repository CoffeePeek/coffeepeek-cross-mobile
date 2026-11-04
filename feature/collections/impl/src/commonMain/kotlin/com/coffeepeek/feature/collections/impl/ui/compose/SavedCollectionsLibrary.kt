package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.SavedCollectionCard
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionsLibrary(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    modifier: Modifier = Modifier, image: @Composable (SavedCollectionSummary, Modifier) -> Unit = { _, m -> CoffeePeekImagePlaceholder(m) }) {
    CoffeePeekScaffold(modifier, topBar = {
        CoffeePeekTopBar(stringResource(Res.string.collections_library), stringResource(Res.string.collections_back),
            onBack = { action(SavedCollectionsAction.Back) }, actions = {
                CoffeePeekPlainIconButton({ action(SavedCollectionsAction.OpenEditor(null)) }, enabled = state.signedIn && !state.busy) {
                    Icon(CoffeePeekIcons.Add, stringResource(Res.string.collections_new_list))
                }
            })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SavedCollectionsStatus(state, action)
            LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) { SavedCollectionsSearch(state, action) }
                SavedCollectionVisibility.entries.reversed().forEach { visibility ->
                    val lists = state.lists.filter { it.visibility == visibility && it.title.contains(state.query, true) }
                    if (lists.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { SavedCollectionSectionTitle(visibility) }
                    items(lists, key = { it.id }) { value ->
                        SavedCollectionCard(value, { action(SavedCollectionsAction.OpenEditor(value.id)) }) { image(value, it) }
                    }
                }
                if (!state.loading && state.signedIn && state.failure != SavedCollectionsFailure.Load && state.lists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(Res.string.collections_library_empty))
                }
                if (state.lists.isNotEmpty() && state.lists.none { it.title.contains(state.query, true) })
                    item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(Res.string.collections_no_matches)) }
            }
        }
    }
}

@Composable
internal fun SavedCollectionsSearch(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit) {
    CoffeePeekSearchField(state.query, { action(SavedCollectionsAction.Query(it)) },
        stringResource(Res.string.collections_search_lists), stringResource(Res.string.collections_clear_search),
        enabled = !state.busy, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun SavedCollectionSectionTitle(visibility: SavedCollectionVisibility) {
    Text(stringResource(if (visibility == SavedCollectionVisibility.Public) Res.string.collections_public_lists else Res.string.collections_private_lists),
        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable private fun SavedCollectionsLibrarySample(dark: Boolean, includePublic: Boolean = true) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionsLibrary(SavedCollectionsState(SavedCollectionsMode.Library, signedIn = true, loading = false,
        lists = listOfNotNull(SavedCollectionSummary("one", "Любимые кофейни", visibility = SavedCollectionVisibility.Public,
            itemCount = 8).takeIf { includePublic }, SavedCollectionSummary("two", "Избранное", itemCount = 3))), {})
}
@Preview @Composable private fun SavedCollectionsLibraryLightPreview() = SavedCollectionsLibrarySample(false)
@Preview @Composable private fun SavedCollectionsLibraryDarkPreview() = SavedCollectionsLibrarySample(true)
@Preview @Composable private fun SavedCollectionsPrivateOnlyLightPreview() = SavedCollectionsLibrarySample(false, includePublic = false)
@Preview @Composable private fun SavedCollectionsPrivateOnlyDarkPreview() = SavedCollectionsLibrarySample(true, includePublic = false)
