package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekPhotoDragStateTest {
    private fun state() = CoffeePeekPhotoDragState().apply {
        bounds[0] = Rect(0f, 0f, 96f, 96f)
        bounds[1] = Rect(104f, 0f, 200f, 96f)
        bounds[2] = Rect(0f, 104f, 96f, 200f)
    }

    @Test fun dropAcrossRowsUsesTheOriginalSlotDespiteTheTranslatedImageBounds() {
        val drag = state()
        val moves = mutableListOf<Pair<Int, Int>>()
        drag.start(0)
        drag.move(Offset(0f, 104f))
        drag.bounds[0] = Rect(0f, 104f, 96f, 200f)
        drag.move(Offset(0f, 1f))
        drag.finish { from, to -> moves += from to to }
        assertEquals(listOf(0 to 2), moves)
        assertEquals(-1, drag.index)
        assertEquals(Offset.Zero, drag.offset)
    }

    @Test fun returningToTheOriginalSlotOrDroppingOutsideDoesNotReorder() {
        val drag = state()
        val moves = mutableListOf<Pair<Int, Int>>()
        drag.start(0)
        drag.move(Offset(104f, 0f))
        drag.move(Offset(-104f, 0f))
        drag.finish { from, to -> moves += from to to }
        drag.start(0)
        drag.move(Offset(104f, 0f))
        drag.move(Offset(300f, 0f))
        drag.finish { from, to -> moves += from to to }
        assertTrue(moves.isEmpty())
    }

    @Test fun cancellingDoesNotCommitTheHoveredDestination() {
        val drag = state()
        val moves = mutableListOf<Pair<Int, Int>>()
        drag.start(2)
        drag.move(Offset(104f, -104f))
        assertEquals(1, drag.target)
        drag.cancel()
        drag.finish { from, to -> moves += from to to }
        assertTrue(moves.isEmpty())
    }
}
