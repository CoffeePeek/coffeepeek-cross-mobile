package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val posterHeight = (maxHeight - if (pages > 1) 192.dp else 144.dp).coerceAtLeast(160.dp)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(Res.string.collections_count, collection.places.size),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (collection.places.isNotEmpty()) {
                item { CollectionFormats(state, onAction) }
                item {
                    Box(Modifier.fillMaxWidth().heightIn(max = posterHeight), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Box(Modifier.clip(RoundedCornerShape(20.dp))) { poster(collection, state.format, page) }
                    }
                }
                if (pages > 1) item { CollectionPosterPages(page, pages, !state.busy) { page = it } }
            }
            if (collection.places.isEmpty()) item { Text(stringResource(Res.string.collections_unavailable)) }
            if (state.publicView) items(CollectionRules.orderedPlaces(collection.places), key = { it.key }) { place ->
                CollectionPlaceRow(place, null, !state.busy) { onAction(CollectionsAction.OpenPlace(place)) }
            }
        }
    }
}

@Composable
private fun CollectionFormats(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    val labels = listOf(stringResource(Res.string.collections_format_chat), stringResource(Res.string.collections_format_story))
    CoffeePeekCapsuleSegmentedControl(CollectionPosterFormat.entries, state.format, { labels[it.ordinal] },
        { onAction(CollectionsAction.Format(it)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy)
}

@Composable
private fun CollectionPosterPages(page: Int, total: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        CoffeePeekPlainIconButton(onClick = { onPage(page - 1) }, enabled = enabled && page > 0) {
            Icon(CoffeePeekIcons.ChevronLeft, stringResource(Res.string.collections_previous_page))
        }
        Text(stringResource(Res.string.collections_page, page + 1, total), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekPlainIconButton(onClick = { onPage(page + 1) }, enabled = enabled && page + 1 < total) {
            Icon(CoffeePeekIcons.ChevronRight, stringResource(Res.string.collections_next_page))
        }
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
