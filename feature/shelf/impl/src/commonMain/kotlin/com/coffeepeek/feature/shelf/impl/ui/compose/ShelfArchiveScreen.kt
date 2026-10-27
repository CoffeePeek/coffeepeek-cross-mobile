package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.component.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveState
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShelfArchiveScreen(viewModel: ShelfArchiveViewModel, image: ShelfImage, modifier: Modifier, onEvent: (ShelfArchiveEvent) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.events.collect { currentEvent(it) } }
    }
    ShelfArchiveScreenContent(state, viewModel::onAction, image, modifier)
}

@Composable
internal fun ShelfArchiveScreenContent(state: ShelfArchiveState, onAction: (ShelfArchiveAction) -> Unit, image: ShelfImage, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    ShelfArchiveMessageEffect(state.message, snackbar, onAction)
    CoffeePeekScaffold(modifier, snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        Column {
            CoffeePeekTopBar(stringResource(Res.string.shelf_archive), stringResource(Res.string.shelf_archive_back),
                onBack = { onAction(ShelfArchiveAction.Back) })
            CoffeePeekSearchField(state.query, { onAction(ShelfArchiveAction.QueryChanged(it)) },
                stringResource(Res.string.shelf_archive_search), stringResource(Res.string.shelf_archive_clear),
                modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
        }
    }) { padding -> ShelfArchiveList(state, onAction, image, Modifier.padding(padding)) }
    state.openBag?.let { item ->
        ShelfBagSheet(item, state.today, image, ShelfBagSheetMode.Archive, onPrimary = { onAction(ShelfArchiveAction.Restore(item.id)) },
            onDelete = { onAction(ShelfArchiveAction.RequestDelete(item.id)) }, onDismiss = { onAction(ShelfArchiveAction.CloseBag) })
    }
    state.deletingId?.let { id ->
        ShelfDeleteConfirmation({ onAction(ShelfArchiveAction.Delete(id)) }, { onAction(ShelfArchiveAction.CancelDelete) })
    }
}

@Composable
private fun ShelfArchiveList(state: ShelfArchiveState, onAction: (ShelfArchiveAction) -> Unit, image: ShelfImage, modifier: Modifier) {
    val listState = rememberLazyListState()
    ShelfArchivePaging(state, listState, onAction)
    val retry = { onAction(ShelfArchiveAction.Retry) }
    LazyColumn(modifier.fillMaxSize(), listState,
        contentPadding = coffeePeekScrollContentPadding(PaddingValues(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))) {
        when {
            state.loadFailed -> item(key = "error") { ShelfLoadError(stringResource(Res.string.shelf_archive_failed), retry) }
            state.loading && state.items.isEmpty() -> item(key = "loading") { ShelfArchiveLoader() }
            state.items.isEmpty() -> item(key = "empty") { ShelfArchiveEmpty(searching = state.query.isNotBlank()) }
        }
        shelfArchiveSections(state, image, onAction)
        if (state.loadingMore) item(key = "more-loading") { ShelfArchiveLoader() }
        if (state.loadMoreFailed) item(key = "more-error") { ShelfLoadError(stringResource(Res.string.shelf_archive_more_failed), retry) }
    }
}

private fun LazyListScope.shelfArchiveSections(state: ShelfArchiveState, image: ShelfImage, onAction: (ShelfArchiveAction) -> Unit) {
    state.sections.forEach { section ->
        item(key = "month-${section.year}-${section.month}") {
            Text(shelfMonthText(section.year, section.month, state.today), Modifier.padding(top = CoffeePeekDimens.spacing4,
                bottom = CoffeePeekDimens.spacing1).semantics { heading() },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(section.items, key = { it.id }) { item ->
            ShelfArchiveRow(item, state.today, image, { onAction(ShelfArchiveAction.OpenBag(item.id)) })
        }
    }
}

/** Asks for the next page when the last rows come into view. */
@Composable
private fun ShelfArchivePaging(state: ShelfArchiveState, listState: LazyListState, onAction: (ShelfArchiveAction) -> Unit) {
    val currentAction by rememberUpdatedState(onAction)
    val ready = state.hasMore && !state.loading && !state.loadingMore && !state.loadMoreFailed
    LaunchedEffect(ready, state.items.size) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: -1) >= it.totalItemsCount - 4 } }
            .collect { nearEnd -> if (nearEnd) currentAction(ShelfArchiveAction.LoadMore) }
    }
}

@Composable
private fun ShelfArchiveLoader() {
    Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center) {
        CoffeePeekLoader(stringResource(Res.string.shelf_loading))
    }
}

@Composable
private fun ShelfArchiveEmpty(searching: Boolean) {
    Column(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        val title = if (searching) Res.string.shelf_archive_not_found else Res.string.shelf_archive_empty
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (!searching) Text(stringResource(Res.string.shelf_archive_empty_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShelfArchiveMessageEffect(message: ShelfArchiveMessage?, snackbar: SnackbarHostState, onAction: (ShelfArchiveAction) -> Unit) {
    val text = when (message) {
        null -> null
        ShelfArchiveMessage.Restored -> stringResource(Res.string.shelf_restored)
        ShelfArchiveMessage.Deleted -> stringResource(Res.string.shelf_deleted)
        is ShelfArchiveMessage.Failed -> message.issue.text()
    }
    val currentAction by rememberUpdatedState(onAction)
    LaunchedEffect(message) {
        if (text == null) return@LaunchedEffect
        snackbar.showSnackbar(text)
        currentAction(ShelfArchiveAction.DismissMessage)
    }
}

internal fun previewArchiveItems(): List<ShelfBagItem> = listOf(
    previewBag("k", "Эспрессо Кения", "River Roasters", 250, finished = true),
    previewBag("h", "Гондурас Мойзес", "Welder Catherine", 200, finished = true),
    previewBag("p", "Перу Санта Роса", null, 250, photo = false, finished = true),
).mapIndexed { index, bag ->
    ShelfBagItem(bag, ShelfDay(2026, 9, 1 + index), ShelfDay(2026, 10 - index, 20), ShelfDay(2026, 8, 25))
}

@Composable
private fun ShelfArchivePreview(dark: Boolean, state: ShelfArchiveState) = CoffeePeekTheme(darkTheme = dark) {
    ShelfArchiveScreenContent(state, {}, PreviewShelfImage)
}

@Preview @Composable private fun ShelfArchiveLightPreview() =
    ShelfArchivePreview(false, ShelfArchiveState(previewToday, items = previewArchiveItems(), loading = false))
@Preview @Composable private fun ShelfArchiveDarkPreview() =
    ShelfArchivePreview(true, ShelfArchiveState(previewToday, items = previewArchiveItems(), loading = false))
@Preview @Composable private fun ShelfArchiveEmptyLightPreview() = ShelfArchivePreview(false, ShelfArchiveState(previewToday, loading = false))
@Preview @Composable private fun ShelfArchiveEmptyDarkPreview() = ShelfArchivePreview(true, ShelfArchiveState(previewToday, loading = false))
