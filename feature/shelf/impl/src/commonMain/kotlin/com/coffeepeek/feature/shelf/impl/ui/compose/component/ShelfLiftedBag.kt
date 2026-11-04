package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val LiftSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
private const val SINK_MILLIS = 280
private const val SINK_SCALE = 0.3f

/**
 * The lifted bag, drawn above everything on the shelf screen: the header, the archive button and the finish zone. It follows
 * the finger, tilted and raised. It is the bag's own tile (see [ShelfTiles]), moved here from the shelf. Let go on the shelf, it settles and flies to its slot, and only then does the shelf show it
 * there; dropped on the zone, it shrinks into the box; if the summary is dismissed, the shelf simply shows it in its slot again.
 * [modifier] must cover the screen; this layer takes no touches.
 */
@Composable
internal fun ShelfLiftedBag(bags: List<ShelfBagItem>, drag: ShelfDragState, tiles: ShelfTiles, modifier: Modifier = Modifier) {
    Box(modifier.onGloballyPositioned { drag.overlayOrigin = it.positionInRoot() }) {
        val id = drag.bagId ?: drag.releasedId ?: drag.archiving ?: return@Box
        val item = bags.firstOrNull { it.id == id } ?: run {
            // The bag left the shelf mid-flight; nothing is left to land.
            SideEffect { drag.clearRelease(id) }
            return@Box
        }
        val geometry = drag.geometry ?: return@Box
        key(id) { ShelfFlyingBag(item, bags, drag, geometry, tiles) }
    }
}

@Composable
private fun ShelfFlyingBag(item: ShelfBagItem, bags: List<ShelfBagItem>, drag: ShelfDragState, geometry: ShelfGridGeometry, tiles: ShelfTiles) {
    val motion = remember { ShelfBagMotion() }
    val held = drag.bagId == item.id
    val sinking = drag.archiving == item.id
    LaunchedEffect(held, sinking) {
        when {
            held -> motion.pickUp()
            sinking -> motion.sink()
            else -> drag.releasedAt(item.id)?.let { from -> motion.land(from); drag.clearRelease(item.id) }
        }
    }
    // The slot is read every frame, as the shelf may scroll or reorder while the bag is on its way.
    val slot = { geometry.slot(drag.order(bags.map { it.id }).indexOf(item.id).coerceAtLeast(0)).topLeft }
    val half = Offset(geometry.columnWidth, geometry.slotHeight) / 2f
    val size = with(LocalDensity.current) { geometry.columnWidth.toDp() to geometry.slotHeight.toDp() }
    tiles.Tile(item, Modifier
        .offset { (motion.position(drag, item.id, slot, half) - drag.overlayOrigin).round() }
        .size(size.first, size.second)
        .graphicsLayer {
            val scale = (1f + 0.1f * motion.lift.value) * (SINK_SCALE + (1f - SINK_SCALE) * motion.presence.value)
            scaleX = scale; scaleY = scale
            rotationZ = -5f * motion.lift.value
            alpha = motion.presence.value.coerceIn(0f, 1f)
        }
        .clearAndSetSemantics {}, elevation = 4.dp + 14.dp * motion.lift.value.coerceAtLeast(0f))
}

/** [lift] tilts and raises the bag, [flight] carries it 0 → 1 along its path and [presence] shrinks it into the box. */
private class ShelfBagMotion {
    val lift = Animatable(0f)
    val flight = Animatable(0f)
    val presence = Animatable(1f)
    /** Where the landing started; null until [land] runs, which is a frame after the finger is lifted. */
    var start by mutableStateOf<Offset?>(null)
        private set

    suspend fun pickUp() {
        presence.snapTo(1f)
        lift.animateTo(1f, LiftSpring)
    }

    /** Into the box: towards the zone's centre, levelling, shrinking and fading. */
    suspend fun sink() = coroutineScope {
        flight.snapTo(0f)
        launch { lift.animateTo(0f, tween(SINK_MILLIS)) }
        launch { presence.animateTo(0f, tween(SINK_MILLIS, easing = FastOutLinearInEasing)) }
        flight.animateTo(1f, tween(SINK_MILLIS, easing = FastOutSlowInEasing))
    }

    /** From where it was let go back to its slot. */
    suspend fun land(from: Offset) = coroutineScope {
        start = from
        flight.snapTo(0f)
        launch { lift.animateTo(0f, LiftSpring) }
        flight.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
    }

    /** In root coordinates: under the finger, on the way into the box, or on the way to the slot. */
    fun position(drag: ShelfDragState, id: String, slot: () -> Offset, half: Offset): Offset = when (id) {
        drag.bagId -> drag.finger - drag.grab
        drag.archiving -> lerp(drag.gridOrigin + drag.sinkFrom, drag.sinkTo - half, flight.value)
        // Until the landing starts, stay where the finger let go: an unset start would draw the bag at the grid's corner.
        else -> drag.gridOrigin + lerp(start ?: drag.releasedAt(id) ?: slot(), slot(), flight.value)
    }
}
