package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.CollectionPlaceRow
import com.coffeepeek.feature.collections.impl.ui.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionPublicContent(state: SavedCollectionsState, action: (SavedCollectionsAction) -> Unit,
    modifier: Modifier = Modifier, image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    val value = state.collection
    CoffeePeekScaffold(modifier, topBar = {
        CoffeePeekTopBar(value?.summary?.title ?: stringResource(Res.string.collections_title),
            stringResource(Res.string.collections_back), onBack = { action(SavedCollectionsAction.Back) })
    }, bottomBar = { if (value != null) SavedPublicActions(value, state.busy, action) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SavedCollectionsStatus(state, action)
            if (value != null) LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { image(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(20.dp))) }
                if (value.summary.description.isNotBlank()) item { Text(value.summary.description, style = MaterialTheme.typography.bodyLarge) }
                item { Text(stringResource(Res.string.collections_saved_count, value.places.size), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                itemsIndexed(value.places, key = { _, place -> place.key }) { index, place ->
                    CollectionPlaceRow(place, null, !state.busy, first = index == 0, last = index == value.places.lastIndex) {
                        action(SavedCollectionsAction.OpenPlace(place))
                    }
                }
                if (value.places.isEmpty()) item { Text(stringResource(Res.string.collections_saved_empty)) }
            }
        }
    }
}

@Composable
private fun SavedPublicActions(value: SavedCollection, busy: Boolean, action: (SavedCollectionsAction) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (value.summary.url != null) CoffeePeekButton(stringResource(Res.string.collections_share_link),
            { action(SavedCollectionsAction.ShareLink) }, enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (value.places.isNotEmpty()) CoffeePeekTextButton({ action(SavedCollectionsAction.Preview) },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.collections_picture)) }
    }
}

@Composable private fun SavedPublicSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionPublicContent(SavedCollectionsState(SavedCollectionsMode.Public, loading = false,
        collection = SavedCollection(SavedCollectionSummary("sample", "Кофейный маршрут", "Для прогулки по городу", SavedCollectionVisibility.Public,
            itemCount = 1), listOf(CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня")))), {})
}
@Preview @Composable private fun SavedPublicLightPreview() = SavedPublicSample(false)
@Preview @Composable private fun SavedPublicDarkPreview() = SavedPublicSample(true)
