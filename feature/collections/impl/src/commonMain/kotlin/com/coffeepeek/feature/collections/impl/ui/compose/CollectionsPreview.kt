package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.CollectionPlaceRow
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionsPreview(state: CollectionsState, onAction: (CollectionsAction) -> Unit,
    poster: @Composable (SharedCollection, CollectionPosterFormat, Int) -> Unit) {
    val collection = state.collection ?: return
    var page by remember(collection.id, state.format) { mutableIntStateOf(0) }
    val pages = ((collection.places.size + state.format.perPage - 1) / state.format.perPage).coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(collection.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text(stringResource(Res.string.collections_count, collection.places.size)) }
        if (collection.places.isNotEmpty() && !state.publicView) {
            item { CollectionFormats(state, onAction) }
            item { poster(collection, state.format, page) }
            if (pages > 1) item { CollectionPosterPages(page, pages, !state.busy) { page = it } }
        }
        item { CollectionShareActions(state, onAction) }
        if (collection.places.isEmpty()) item { Text(stringResource(Res.string.collections_unavailable)) }
        items(CollectionRules.orderedPlaces(collection.places), key = { it.key }) { place ->
            CollectionPlaceRow(place, null, !state.busy) { onAction(CollectionsAction.OpenPlace(place)) }
        }
    }
}

@Composable
private fun CollectionFormats(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CollectionPosterFormat.entries.forEach { format ->
            CoffeePeekTextButton(onClick = { onAction(CollectionsAction.Format(format)) }, enabled = !state.busy) {
                Text(stringResource(if (format == CollectionPosterFormat.Chat) Res.string.collections_format_chat else Res.string.collections_format_story),
                    fontWeight = if (format == state.format) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun CollectionShareActions(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekButton(stringResource(Res.string.collections_share_link), { onAction(CollectionsAction.ShareLink) }, enabled = !state.busy)
        CoffeePeekButton(stringResource(if (state.busy) Res.string.collections_preparing else Res.string.collections_share_image),
            { onAction(CollectionsAction.ShareImages) }, enabled = !state.busy && state.collection?.places?.isNotEmpty() == true)
    }
}

@Composable
private fun CollectionPosterPages(page: Int, total: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CoffeePeekIconButton(onClick = { onPage(page - 1) }, enabled = enabled && page > 0,
            contentDescription = stringResource(Res.string.collections_previous_page)) { Icon(CoffeePeekIcons.ChevronLeft, null) }
        Text(stringResource(Res.string.collections_page, page + 1, total))
        CoffeePeekIconButton(onClick = { onPage(page + 1) }, enabled = enabled && page + 1 < total,
            contentDescription = stringResource(Res.string.collections_next_page)) { Icon(CoffeePeekIcons.ChevronRight, null) }
    }
}

@Composable
private fun CollectionsPreviewSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionsPreview(CollectionsState(loading = false, publicView = true, page = CollectionsPage.Preview,
        collection = SharedCollection("sample", "Мой кофейный Минск", "https://coffeepeek.by",
            listOf(CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня")))), {}, { _, _, _ -> })
}
@Preview @Composable private fun CollectionsPreviewLightPreview() = CollectionsPreviewSample(false)
@Preview @Composable private fun CollectionsPreviewDarkPreview() = CollectionsPreviewSample(true)
