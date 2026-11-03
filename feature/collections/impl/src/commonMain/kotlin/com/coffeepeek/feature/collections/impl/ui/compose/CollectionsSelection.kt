package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
    val focus = LocalFocusManager.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)) {
        item { CoffeePeekTextField(stringResource(Res.string.collections_name), state.title,
            { onAction(CollectionsAction.Title(it)) }, defaultTitle, enabled = !state.busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })) }
        CollectionPlaceKind.entries.forEach { kind ->
            val places = state.places.filter { it.kind == kind }
            if (places.isNotEmpty()) item(key = "heading:$kind") {
                Text(stringResource(if (kind == CollectionPlaceKind.CoffeeShop) Res.string.collections_shops else Res.string.collections_roasters),
                    Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall)
            }
            itemsIndexed(places, key = { _, place -> place.key }) { index, place ->
                CollectionPlaceRow(place, place.key in state.selected, !state.busy, first = index == 0, last = index == places.lastIndex) {
                    onAction(CollectionsAction.Toggle(place.key))
                }
            }
        }
        if (state.places.isEmpty()) item { Text(stringResource(Res.string.collections_empty)) }
    }
}

@Composable
private fun CollectionsSelectionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CollectionsSelection(CollectionsState(signedIn = true, loading = false,
        selected = setOf("CoffeeShop:sample", "Roaster:roaster"), places = listOf(
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня"),
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "second", "Кофейная лаборатория"),
            CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Любимый обжарщик"))), {})
}
@Preview @Composable private fun CollectionsSelectionLightPreview() = CollectionsSelectionPreview(false)
@Preview @Composable private fun CollectionsSelectionDarkPreview() = CollectionsSelectionPreview(true)
