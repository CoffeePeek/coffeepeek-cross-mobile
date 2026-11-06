package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePeekActionMenuPositionTest {
    private val window = IntSize(400, 800)
    private val menu = IntSize(304, 120)

    @Test fun alignsToTriggerAndStaysBelowIt() {
        assertEquals(IntOffset(80, 100), position(IntRect(336, 44, 384, 92)))
    }

    @Test fun opensAboveTriggerWhenThereIsNoSpaceBelow() {
        assertEquals(IntOffset(80, 572), position(IntRect(336, 700, 384, 748)))
    }

    @Test fun clampsToWindowEdgesAndSupportsRtl() {
        val anchor = IntRect(16, 44, 64, 92)
        assertEquals(IntOffset(8, 100), position(anchor))
        assertEquals(IntOffset(16, 100), position(anchor, LayoutDirection.Rtl))
    }

    private fun position(anchor: IntRect, direction: LayoutDirection = LayoutDirection.Ltr) =
        coffeePeekActionMenuPosition(anchor, window, direction, menu, margin = 8)
}
