package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ShelfGap = 16.dp
private val ShelfRowGap = 28.dp
private const val SLOT_ASPECT = 1.35f

internal fun shelfGeometry(width: Float, density: Density, rtl: Boolean): ShelfGridGeometry = with(density) {
    val gap = ShelfGap.toPx()
    val slot = (width - gap * (SHELF_COLUMNS - 1)) / SHELF_COLUMNS * SLOT_ASPECT
    ShelfGridGeometry(width, gap, slot, slot + ShelfLedgeHeight.toPx() + ShelfRowGap.toPx(), rtl)
}

/**
 * Bags in rows of three, standing on ledges. A tap opens a bag; a long press lifts it to move it or to drop it on the
 * finish zone. TalkBack gets the same moves and finish as custom actions.
 */
@Composable
internal fun ShelfGrid(bags: List<ShelfBagItem>, drag: ShelfDragState, image: ShelfImage, onAction: (ShelfAction) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val geometry = remember(constraints.maxWidth, density, rtl) { shelfGeometry(constraints.maxWidth.toFloat(), density, rtl) }
        val byId = bags.associateBy { it.id }
        val shown = drag.order(bags.map { it.id }).mapNotNull(byId::get)
        val currentShown by rememberUpdatedState(shown)
        val haptics = LocalHapticFeedback.current
        Box(Modifier.fillMaxWidth().height(with(density) { (geometry.rows(bags.size) * geometry.rowHeight).toDp() })
            .onGloballyPositioned { drag.gridOrigin = it.positionInRoot() }
            .shelfGestures(drag, geometry, { currentShown }, haptics, onAction)) {
            repeat(geometry.rows(bags.size)) { row -> ShelfLedge(Modifier.offset { IntOffset(0, (row * geometry.rowHeight + geometry.slotHeight).toInt()) }) }
            if (drag.active) ShelfGhost(geometry.slot(drag.target), density)
            shown.forEachIndexed { index, item ->
                key(item.id) { ShelfGridTile(item, index, shown.size, geometry, drag, image, onAction) }
            }
        }
    }
}

@Composable
private fun ShelfGridTile(
    item: ShelfBagItem, index: Int, count: Int, geometry: ShelfGridGeometry, drag: ShelfDragState,
    image: ShelfImage, onAction: (ShelfAction) -> Unit,
) {
    val held = drag.bagId == item.id
    val slot = geometry.slot(index).topLeft.round()
    val position = remember { Animatable(slot, IntOffset.VectorConverter) }
    LaunchedEffect(slot, held) {
        if (held) return@LaunchedEffect
        drag.takeRelease(item.id)?.let { position.snapTo(it.round()) }
        position.animateTo(slot)
    }
    val size = with(LocalDensity.current) { geometry.columnWidth.toDp() to geometry.slotHeight.toDp() }
    ShelfBagTile(item, image, Modifier
        .offset { if (held) (drag.pointer - drag.grab).round() else position.value }
        .size(size.first, size.second)
        .zIndex(if (held) 1f else 0f)
        .scale(if (held) 1.06f else 1f)
        .shelfTileSemantics(item.id, index, count, onAction))
}

@Composable
private fun Modifier.shelfTileSemantics(id: String, index: Int, count: Int, onAction: (ShelfAction) -> Unit): Modifier {
    val open = stringResource(Res.string.shelf_bag_open)
    val up = stringResource(Res.string.shelf_move_up)
    val down = stringResource(Res.string.shelf_move_down)
    val finish = stringResource(Res.string.shelf_finish_action)
    return semantics(mergeDescendants = true) {
        onClick(open) { onAction(ShelfAction.OpenBag(id)); true }
        customActions = listOfNotNull(
            CustomAccessibilityAction(up) { onAction(ShelfAction.Move(id, index - 1)); true }.takeIf { index > 0 },
            CustomAccessibilityAction(down) { onAction(ShelfAction.Move(id, index + 1)); true }.takeIf { index < count - 1 },
            CustomAccessibilityAction(finish) { onAction(ShelfAction.StartFinish(id)); true },
        )
    }
}

/** Where the lifted bag will land. */
@Composable
private fun ShelfGhost(slot: Rect, density: Density) {
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    val colors = MaterialTheme.colorScheme
    with(density) {
        Box(Modifier.offset { IntOffset(slot.left.toInt(), (slot.top + slot.height * 0.18f).toInt()) }
            .size(slot.width.toDp(), (slot.height * 0.82f).toDp())
            .background(colors.onSurface.copy(alpha = 0.06f), shape).border(1.5.dp, colors.outline, shape))
    }
}

/**
 * One gesture per touch: a tap on a bag opens it; a long press lifts it and the drag ends in a move, a finish or nothing.
 * A scroll that starts before the long press cancels it.
 */
private fun Modifier.shelfGestures(
    dragState: ShelfDragState, geometry: ShelfGridGeometry, shown: () -> List<ShelfBagItem>,
    haptics: HapticFeedback, onAction: (ShelfAction) -> Unit,
): Modifier = pointerInput(geometry) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val bags = shown()
        val index = geometry.bagAt(down.position, bags.size)?.takeIf { dragState.settling == null } ?: return@awaitEachGesture
        val press = awaitLongPressOrCancellation(down.id)
        if (press == null) {
            val up = currentEvent.changes.firstOrNull { it.id == down.id }
            if (up != null && up.changedToUp() && geometry.slot(index).contains(up.position)) onAction(ShelfAction.OpenBag(bags[index].id))
            return@awaitEachGesture
        }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        dragState.start(bags[index].id, index, press.position, geometry, bags.size)
        val completed = drag(press.id) { change -> dragState.drag(change.positionChange()); change.consume() }
        if (!completed) { dragState.cancel(); return@awaitEachGesture }
        when (val drop = dragState.release()) {
            is ShelfDrop.Move -> onAction(ShelfAction.Move(drop.id, drop.toIndex))
            is ShelfDrop.Finish -> onAction(ShelfAction.StartFinish(drop.id))
            null -> Unit
        }
    }
}

@Composable
private fun ShelfGridPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
        ShelfGrid(previewShelfItems(), remember { ShelfDragState() }, PreviewShelfImage, {})
    }
}

@Preview @Composable private fun ShelfGridLightPreview() = ShelfGridPreview(false)
@Preview @Composable private fun ShelfGridDarkPreview() = ShelfGridPreview(true)
