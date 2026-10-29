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
 * A bag lifted by a long press. [pointer] is the finger in grid coordinates; [gridOrigin], [overlayOrigin] (the layer that draws
 * the lifted bag above the screen) and [zone] (the "Закончилась" drop zone) are root coordinates that layout keeps current.
 * After a move the new order is kept in [settling] until the shelf list catches up, so the bags do not jump back for a frame.
 * A drop on the zone puts the bag in the box: it is [archiving] until it leaves the shelf or the summary is dismissed.
 */
@Stable
internal class ShelfDragState {
    var bagId by mutableStateOf<String?>(null)
        private set
    var target by mutableIntStateOf(-1)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    /** The finger in root coordinates; unlike [pointer] it stays put when the shelf scrolls under it. */
    var finger by mutableStateOf(Offset.Zero)
        private set
    var overZone by mutableStateOf(false)
        private set
    var settling by mutableStateOf<ShelfDrop.Move?>(null)
        private set
    /** The finger's offset from the lifted slot's top-left corner. */
    var grab = Offset.Zero
        private set
    var gridOrigin = Offset.Zero
    var overlayOrigin = Offset.Zero
    /** The grid of the last lift; it stays after the drop, so the bag can fly back to its slot. */
    var geometry: ShelfGridGeometry? = null
        private set
    var zone: Rect? = null
    private var from = -1
    private var count = 0
    private var released by mutableStateOf<Pair<String, Offset>?>(null)
    /** Where a bag dropped on the zone was let go (grid) and where it sank (root, the zone's centre). */
    var sinkFrom = Offset.Zero
        private set
    var sinkTo = Offset.Zero
        private set

    /** The bag dropped on the zone, waiting in the box while its summary is open; its slot stays reserved. */
    var archiving by mutableStateOf<String?>(null)
        private set

    val active: Boolean get() = bagId != null
    val rootPointer: Offset get() = gridOrigin + pointer

    /** The bag on its way back to the shelf after a drop, until [clearRelease]. */
    val releasedId: String? get() = released?.first

    fun start(id: String, index: Int, at: Offset, geometry: ShelfGridGeometry, count: Int) {
        this.geometry = geometry
        this.count = count
        from = index
        target = index
        pointer = at
        finger = gridOrigin + at
        grab = at - geometry.slot(index).topLeft
        overZone = false
        settling = null
        released = null
        bagId = id
    }

    fun drag(delta: Offset) {
        if (!active) return
        pointer += delta
        finger += delta
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
        if (drop is ShelfDrop.Finish) {
            sinkFrom = pointer - grab
            sinkTo = zone?.center ?: rootPointer
            archiving = id
        } else released = id to pointer - grab
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

    /** The dropped bag left the shelf. */
    fun archived(id: String) {
        if (archiving == id) archiving = null
    }

    /** The summary was dismissed and the bag stays: it is simply back in its slot, where its outline kept the place. */
    fun archiveCancelled(id: String) {
        if (archiving == id) archiving = null
    }

    /**
     * Where the bag [id] was let go. It is state, so the first frame after release already draws the bag there; the lifted
     * layer then flies it to its slot and calls [clearRelease], and the shelf shows it there again.
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
        zone = null
    }
}
