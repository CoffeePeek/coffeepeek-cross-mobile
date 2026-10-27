package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShelfDragStateTest {
    // Columns of 100 px with 10 px gaps; slots 140 px high in rows of 200 px.
    private val geometry = ShelfGridGeometry(width = 320f, gap = 10f, slotHeight = 140f, rowHeight = 200f)

    @Test fun slotsFillRowsOfThreeAndMirrorInRtl() {
        assertEquals(Rect(110f, 200f, 210f, 340f), geometry.slot(4))
        assertEquals(Rect(220f, 0f, 320f, 140f), geometry.copy(rtl = true).slot(0))
        assertEquals(2, geometry.rows(4))
    }

    @Test fun onlyBagsAreHitAndDraggingClampsToTheShelf() {
        assertEquals(4, geometry.bagAt(Offset(150f, 250f), count = 5))
        assertNull(geometry.bagAt(Offset(150f, 170f), count = 5))
        assertNull(geometry.bagAt(Offset(250f, 250f), count = 5))
        assertEquals(4, geometry.slotNear(Offset(300f, 900f), count = 5))
        assertEquals(0, geometry.slotNear(Offset(-40f, -60f), count = 5))
        assertEquals(1, geometry.slotNear(Offset(106f, 10f), count = 5))
    }

    @Test fun dropOnAnotherSlotMovesAndKeepsTheNewOrderUntilSettled() {
        val drag = ShelfDragState()
        drag.start("a", 0, Offset(50f, 70f), geometry, count = 4)
        drag.drag(Offset(110f, 200f))
        assertEquals(listOf("b", "c", "d", "a"), drag.order(listOf("a", "b", "c", "d")))
        assertEquals(ShelfDrop.Move("a", 3), drag.release())
        assertFalse(drag.active)
        assertEquals(listOf("b", "c", "d", "a"), drag.order(listOf("a", "b", "c", "d")))
        assertEquals(Offset(110f, 200f), drag.takeRelease("a"))
        assertNull(drag.takeRelease("a"))
        drag.settled()
        assertEquals(listOf("a", "b", "c", "d"), drag.order(listOf("a", "b", "c", "d")))
    }

    @Test fun dropOnTheFinishZoneFinishesAndKeepsTheBagInPlace() {
        val drag = ShelfDragState().apply { gridOrigin = Offset(0f, 100f); zone = Rect(0f, 700f, 320f, 780f) }
        drag.start("b", 1, Offset(150f, 50f), geometry, count = 3)
        drag.drag(Offset(0f, 600f))
        assertTrue(drag.overZone)
        assertEquals(1, drag.target)
        assertEquals(ShelfDrop.Finish("b"), drag.release())
        assertNull(drag.zone)
    }

    @Test fun releasingOnTheSameSlotOrCancellingDoesNothing() {
        val drag = ShelfDragState()
        drag.start("a", 0, Offset(50f, 70f), geometry, count = 3)
        drag.drag(Offset(5f, 5f))
        assertNull(drag.release())
        drag.start("a", 0, Offset(50f, 70f), geometry, count = 3)
        drag.scrolled(220f)
        assertEquals(2, drag.target)
        drag.cancel()
        assertFalse(drag.active)
        assertNull(drag.settling)
    }
}
