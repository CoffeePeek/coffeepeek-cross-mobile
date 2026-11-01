package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ShelfGap = 16.dp
private val ShelfRowGap = 28.dp
private const val SLOT_ASPECT = 1.35f
private const val GHOST_FADE_MILLIS = 180

internal fun shelfGeometry(width: Float, density: Density, rtl: Boolean): ShelfGridGeometry = with(density) {
    val gap = ShelfGap.toPx()
    val slot = (width - gap * (SHELF_COLUMNS - 1)) / SHELF_COLUMNS * SLOT_ASPECT
    ShelfGridGeometry(width, gap, slot, slot + (ShelfLedgeHeight - ShelfLedgeOverlap).toPx() + ShelfRowGap.toPx(), rtl)
}

/**
 * Bags in rows of three behind frosted front rails. A tap opens a bag; a long press lifts it to move it or to drop it on the
 * finish zone. TalkBack gets the same moves and finish as custom actions.
 */
@Composable
internal fun ShelfGrid(
    bags: List<ShelfBagItem>, drag: ShelfDragState, tiles: ShelfTiles, image: ShelfImage, onAction: (ShelfAction) -> Unit,
    modifier: Modifier = Modifier, arrivingId: String? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val geometry = remember(constraints.maxWidth, density, rtl) { shelfGeometry(constraints.maxWidth.toFloat(), density, rtl) }
        val byId = bags.associateBy { it.id }
        val shown = drag.order(bags.map { it.id }).mapNotNull(byId::get)
        val currentShown by rememberUpdatedState(shown)
        val haptics = LocalHapticFeedback.current
        val backdrop = rememberHazeState()
        Box(Modifier.fillMaxWidth().height(with(density) { (geometry.rows(bags.size) * geometry.rowHeight).toDp() })
            .onGloballyPositioned { drag.gridOrigin = it.positionInRoot() }
            .shelfGestures(drag, geometry, { currentShown }, haptics, onAction)) {
            Box(Modifier.matchParentSize().hazeSource(backdrop)) {
                // Only bags feed the backdrop; front rails must never capture themselves.
                val ghost = drag.bagId?.let { it to drag.target } ?: drag.archiving?.let { id -> id to shown.indexOfFirst { it.id == id } }
                ghost?.takeIf { it.second >= 0 }?.let { (id, index) -> byId[id]?.let { ShelfGhost(it, image, geometry.slot(index)) } }
                shown.forEachIndexed { index, item ->
                    key(item.id) { ShelfGridTile(item, index, shown.size, geometry, drag, tiles, onAction, arriving = item.id == arrivingId) }
                }
            }
            val overlap = with(density) { ShelfLedgeOverlap.toPx() }
            repeat(geometry.rows(bags.size)) { row ->
                ShelfLedge(Modifier.offset { IntOffset(0, (row * geometry.rowHeight + geometry.slotHeight - overlap).toInt()) }, backdrop)
            }
        }
    }
}

@Composable
private fun ShelfGridTile(
    item: ShelfBagItem, index: Int, count: Int, geometry: ShelfGridGeometry, drag: ShelfDragState,
    tiles: ShelfTiles, onAction: (ShelfAction) -> Unit, arriving: Boolean,
) {
    // While lifted, in the archive box or flying back, the bag's own tile is in ShelfLiftedBag above the screen; its slot waits.
    val away = drag.bagId == item.id || drag.releasedId == item.id || drag.archiving == item.id
    // A bag just added drops onto the top shelf and springs on the ledge.
    val arrival = rememberShelfLanding(item.id, arriving)
    val fall = with(LocalDensity.current) { 420.dp.toPx() }
    val slot = geometry.slot(index).topLeft.round()
    val position = remember { Animatable(slot, IntOffset.VectorConverter) }
    LaunchedEffect(slot, away) { if (away) position.snapTo(slot) else position.animateTo(slot) }
    if (away) return
    val size = with(LocalDensity.current) { geometry.columnWidth.toDp() to geometry.slotHeight.toDp() }
    tiles.Tile(item, Modifier
        .offset { position.value }
        .shelfLanding(arrival, fall)
        .size(size.first, size.second)
        .shelfTileSemantics(item.id, index, count, onAction), highlighted = arriving)
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

/** The lifted bag's faded, outlined copy in the slot it will land in; it slides along as the target changes. */
@Composable
private fun ShelfGhost(item: ShelfBagItem, image: ShelfImage, slot: Rect) {
    val target = slot.topLeft.round()
    val position = remember { Animatable(target, IntOffset.VectorConverter) }
    LaunchedEffect(target) { position.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow)) }
    // A copy of its own, so it fades in instead of popping up while its card loads.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(GHOST_FADE_MILLIS)) }
    val size = with(LocalDensity.current) { slot.width.toDp() to slot.height.toDp() }
    ShelfBagTile(item, image, Modifier.offset { position.value }.size(size.first, size.second)
        .graphicsLayer { alpha = appear.value }.clearAndSetSemantics {}, ghost = true)
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
            // A scroll ends the wait on a consumed move, so a release here is a tap. iOS hands it over already consumed,
            // which is why consumption is not asked about.
            val up = currentEvent.changes.firstOrNull { it.id == down.id }
            if (up != null && up.changedToUpIgnoreConsumed() && geometry.slot(index).contains(up.position)) onAction(ShelfAction.OpenBag(bags[index].id))
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
private fun ShelfGridPreview(dark: Boolean, opaque: Boolean = false) {
    CompositionLocalProvider(LocalCoffeePeekOpaqueGlassOverride provides opaque) {
        CoffeePeekTheme(darkTheme = dark) {
            Box(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
                ShelfGrid(previewShelfItems(), remember { ShelfDragState() }, rememberShelfTiles(PreviewShelfImage), PreviewShelfImage, {})
            }
        }
    }
}

@Preview @Composable private fun ShelfGridLightPreview() = ShelfGridPreview(false)
@Preview @Composable private fun ShelfGridDarkPreview() = ShelfGridPreview(true)
@Preview @Composable private fun ShelfGridOpaqueLightPreview() = ShelfGridPreview(false, true)
@Preview @Composable private fun ShelfGridOpaqueDarkPreview() = ShelfGridPreview(true, true)
