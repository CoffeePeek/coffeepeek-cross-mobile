package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal const val SHELF_COLUMNS = 3

/** Slots of the shelf grid in pixels: three equal columns; a row is a slot, its ledge and the gap below. */
internal data class ShelfGridGeometry(val width: Float, val gap: Float, val slotHeight: Float, val rowHeight: Float, val rtl: Boolean = false) {
    val columnWidth: Float get() = (width - gap * (SHELF_COLUMNS - 1)) / SHELF_COLUMNS

    fun rows(count: Int): Int = (count + SHELF_COLUMNS - 1) / SHELF_COLUMNS

    fun slot(index: Int): Rect {
        val column = (index % SHELF_COLUMNS).let { if (rtl) SHELF_COLUMNS - 1 - it else it }
        val left = column * (columnWidth + gap)
        val top = index / SHELF_COLUMNS * rowHeight
        return Rect(left, top, left + columnWidth, top + slotHeight)
    }

    /** The bag under [point]; null between or below the bags. */
    fun bagAt(point: Offset, count: Int): Int? = (0 until count).firstOrNull { slot(it).contains(point) }

    /** Where a dragged bag would land; past the last bag it goes to the end. */
    fun slotNear(point: Offset, count: Int): Int {
        val row = (point.y / rowHeight).toInt().coerceAtLeast(0)
        val raw = ((point.x + gap / 2) / (columnWidth + gap)).toInt().coerceIn(0, SHELF_COLUMNS - 1)
        val column = if (rtl) SHELF_COLUMNS - 1 - raw else raw
        return (row * SHELF_COLUMNS + column).coerceIn(0, count - 1)
    }
}

internal sealed interface ShelfDrop {
    val id: String
    data class Move(override val id: String, val toIndex: Int) : ShelfDrop
    data class Finish(override val id: String) : ShelfDrop
}

/**
 * A bag lifted by a long press. [pointer] is the finger in grid coordinates; [gridOrigin] and [zone] (the "Закончилась"
 * drop zone) are root coordinates that layout keeps current. After a move the new order is kept in [settling] until the
 * shelf list catches up, so the bags do not jump back for a frame.
 */
@Stable
internal class ShelfDragState {
    var bagId by mutableStateOf<String?>(null)
        private set
    var target by mutableIntStateOf(-1)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var overZone by mutableStateOf(false)
        private set
    var settling by mutableStateOf<ShelfDrop.Move?>(null)
        private set
    /** The finger's offset from the lifted slot's top-left corner. */
    var grab = Offset.Zero
        private set
    var gridOrigin = Offset.Zero
    var zone: Rect? = null
    private var from = -1
    private var count = 0
    private var geometry: ShelfGridGeometry? = null
    private var released by mutableStateOf<Pair<String, Offset>?>(null)

    val active: Boolean get() = bagId != null
    val rootPointer: Offset get() = gridOrigin + pointer

    fun start(id: String, index: Int, at: Offset, geometry: ShelfGridGeometry, count: Int) {
        this.geometry = geometry
        this.count = count
        from = index
        target = index
        pointer = at
        grab = at - geometry.slot(index).topLeft
        overZone = false
        settling = null
        bagId = id
    }

    fun drag(delta: Offset) {
        if (!active) return
        pointer += delta
        retarget()
    }

    /** The grid scrolled by [pixels] under a still finger. */
    fun scrolled(pixels: Float) {
        if (!active || pixels == 0f) return
        pointer += Offset(0f, pixels)
        retarget()
    }

    fun release(): ShelfDrop? {
        val id = bagId ?: return null
        val drop = when {
            overZone -> ShelfDrop.Finish(id)
            target != from -> ShelfDrop.Move(id, target)
            else -> null
        }
        released = id to pointer - grab
        settling = drop as? ShelfDrop.Move
        reset()
        return drop
    }

    fun cancel() {
        released = bagId?.let { it to pointer - grab }
        reset()
    }

    fun settled() {
        settling = null
    }

    /**
     * Where the bag [id] was let go. It is state, so the first frame after release already draws the bag there; the tile
     * then moves its own animation to this point and calls [clearRelease], and glides on to its slot.
     */
    fun releasedAt(id: String): Offset? = released?.takeIf { it.first == id }?.second

    fun clearRelease(id: String) {
        if (released?.first == id) released = null
    }

    /** [ids] as drawn: the lifted or just-dropped bag at its new index. */
    fun order(ids: List<String>): List<String> {
        val moving = bagId?.let { ShelfDrop.Move(it, target) } ?: settling ?: return ids
        if (moving.id !in ids) return ids
        return ids.filterNot { it == moving.id }.toMutableList().apply { add(moving.toIndex.coerceIn(0, size), moving.id) }
    }

    private fun retarget() {
        val geometry = geometry ?: return
        overZone = zone?.contains(rootPointer) == true
        target = if (overZone) from else geometry.slotNear(pointer, count)
    }

    private fun reset() {
        bagId = null
        from = -1
        target = -1
        overZone = false
        geometry = null
        zone = null
    }
}
