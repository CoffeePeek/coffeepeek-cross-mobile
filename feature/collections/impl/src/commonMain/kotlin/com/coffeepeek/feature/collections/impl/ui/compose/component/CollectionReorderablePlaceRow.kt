package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.CollectionsAction
import org.jetbrains.compose.ui.tooling.preview.Preview
import sh.calvin.reorderable.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun LazyItemScope.CollectionReorderablePlaceRow(places: List<CollectionPlace>, index: Int, checked: Boolean,
    enabled: Boolean, kindAllowed: Boolean, reorder: ReorderableLazyListState,
    onDragging: (CollectionPlaceKind?) -> Unit, onAction: (CollectionsAction) -> Unit) {
    val place = places[index]
    val actions = collectionMoveActions(places, index, enabled, onAction)
    ReorderableItem(reorder, place.key, enabled = enabled && kindAllowed) { dragging ->
        val handle = if (enabled && kindAllowed) Modifier.draggableHandle(
            onDragStarted = { onDragging(place.kind) }, onDragStopped = { onDragging(null) }) else Modifier
        CollectionPlaceRow(place, checked, enabled, first = dragging || index == 0, last = dragging || index == places.lastIndex,
            modifier = Modifier.shadow(if (dragging) 6.dp else 0.dp, RoundedCornerShape(16.dp)), moveActions = actions,
            trailing = { CollectionDragHandle(handle) }, onClick = { onAction(CollectionsAction.Toggle(place.key)) })
    }
}

@Composable
private fun CollectionDragHandle(modifier: Modifier) {
    Box(Modifier.size(48.dp).then(modifier).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.DragHandle, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun collectionMoveActions(places: List<CollectionPlace>, index: Int, enabled: Boolean,
    onAction: (CollectionsAction) -> Unit): List<CustomAccessibilityAction> {
    val earlier = stringResource(Res.string.collections_move_up)
    val later = stringResource(Res.string.collections_move_down)
    if (!enabled) return emptyList()
    val key = places[index].key
    return buildList {
        if (index > 0) add(CustomAccessibilityAction(earlier) {
            onAction(CollectionsAction.Move(key, places[index - 1].key, false))
            true
        })
        if (index < places.lastIndex) add(CustomAccessibilityAction(later) {
            onAction(CollectionsAction.Move(key, places[index + 1].key, true))
            true
        })
    }
}

@Composable
private fun CollectionReorderablePlaceRowSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val places = listOf(CollectionPlace(CollectionPlaceKind.CoffeeShop, "one", "Первая кофейня"),
        CollectionPlace(CollectionPlaceKind.CoffeeShop, "two", "Вторая кофейня"))
    val lazy = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazy) { _, _ -> }
    LazyColumn(state = lazy) {
        itemsIndexed(places, key = { _, place -> place.key }) { index, _ ->
            CollectionReorderablePlaceRow(places, index, index == 0, true, true, reorder, {}, {})
        }
    }
}
@Preview @Composable private fun CollectionReorderablePlaceRowLightPreview() = CollectionReorderablePlaceRowSample(false)
@Preview @Composable private fun CollectionReorderablePlaceRowDarkPreview() = CollectionReorderablePlaceRowSample(true)
