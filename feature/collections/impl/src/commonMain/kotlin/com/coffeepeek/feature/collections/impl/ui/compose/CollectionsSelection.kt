package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
internal fun CollectionsSelection(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    val defaultTitle = stringResource(Res.string.collections_default_title)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(Res.string.collections_intro), style = MaterialTheme.typography.bodyMedium) }
        item { CoffeePeekTextField(stringResource(Res.string.collections_name), state.title,
            { onAction(CollectionsAction.Title(it)) }, defaultTitle, enabled = !state.busy) }
        item { CollectionSelectionActions(state, onAction) }
        items(state.places, key = { it.key }) { place ->
            CollectionPlaceRow(place, place.key in state.selected, !state.busy) { onAction(CollectionsAction.Toggle(place.key)) }
        }
        item { CoffeePeekButton(if (state.busy) stringResource(Res.string.collections_preparing) else stringResource(Res.string.collections_create),
            { onAction(CollectionsAction.Create(defaultTitle)) }, enabled = !state.busy && state.selected.isNotEmpty() &&
                CollectionRules.validTitle(state.title.ifBlank { defaultTitle })) }
        if (state.places.isEmpty()) item { Text(stringResource(Res.string.collections_empty)) }
    }
}

@Composable
private fun CollectionSelectionActions(state: CollectionsState, onAction: (CollectionsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.collections_selected, state.selected.size, CollectionRules.maxPlaces),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CoffeePeekTextButton(onClick = { onAction(CollectionsAction.SelectAll) }, enabled = !state.busy) {
                Text(when {
                    state.selected.isNotEmpty() -> stringResource(Res.string.collections_clear)
                    state.places.size > CollectionRules.maxPlaces -> stringResource(Res.string.collections_select_limit, CollectionRules.maxPlaces)
                    else -> stringResource(Res.string.collections_select_all)
                })
            }
            CoffeePeekTextButton(onClick = { onAction(CollectionsAction.OpenLinks) }, enabled = !state.busy) {
                Text(stringResource(Res.string.collections_links))
            }
        }
    }
}

@Composable
private fun CollectionsSelectionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionsSelection(CollectionsState(signedIn = true, loading = false,
        places = listOf(CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня"))), {})
}
@Preview @Composable private fun CollectionsSelectionLightPreview() = CollectionsSelectionPreview(false)
@Preview @Composable private fun CollectionsSelectionDarkPreview() = CollectionsSelectionPreview(true)
