package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CoffeePeekScaffoldInsetsTest {
    @Test
    fun androidAndMainTabPolicyPreservesMaterialPadding() {
        val padding = PaddingValues(start = 11.dp, top = 23.dp, end = 17.dp, bottom = 34.dp)
        val inactive = CoffeePeekScreenInsets(bottom = 34.dp, extendsToBottom = false)
        assertSame(padding, coffeePeekScaffoldContentPadding(padding, inactive, hasBottomBar = false))
        assertSame(padding, coffeePeekScrollContentPadding(padding, inactive))
    }

    @Test
    fun internalViewportKeepsHeaderAndBothLayoutDirections() {
        val padding = PaddingValues(start = 11.dp, top = 64.dp, end = 17.dp, bottom = 34.dp)
        val viewport = coffeePeekScaffoldContentPadding(padding, CoffeePeekScreenInsets(34.dp, true), false)
        assertEquals(64.dp, viewport.calculateTopPadding())
        assertEquals(0.dp, viewport.calculateBottomPadding())
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            assertEquals(padding.calculateLeftPadding(direction), viewport.calculateLeftPadding(direction))
            assertEquals(padding.calculateRightPadding(direction), viewport.calculateRightPadding(direction))
        }
    }

    @Test
    fun bottomActionBarStillReservesItsEntireMeasuredHeight() {
        val padding = PaddingValues(top = 64.dp, bottom = 112.dp)
        assertSame(padding, coffeePeekScaffoldContentPadding(padding, CoffeePeekScreenInsets(34.dp, true), true))
    }

    @Test
    fun movingSafeAreaInsideScrollPreservesLastItemClearance() {
        val safeArea = CoffeePeekScreenInsets(34.dp, true)
        val outer = PaddingValues(top = 64.dp, bottom = 34.dp)
        val content = PaddingValues(start = 16.dp, top = 8.dp, end = 20.dp, bottom = 24.dp)
        val viewport = coffeePeekScaffoldContentPadding(outer, safeArea, false)
        val scroll = coffeePeekScrollContentPadding(content, safeArea)
        assertEquals(outer.calculateBottomPadding() + content.calculateBottomPadding(),
            viewport.calculateBottomPadding() + scroll.calculateBottomPadding())
        assertEquals(content.calculateTopPadding(), scroll.calculateTopPadding())
        assertEquals(content.calculateLeftPadding(LayoutDirection.Rtl), scroll.calculateLeftPadding(LayoutDirection.Rtl))
    }

    @Test
    fun zeroSafeAreaKeepsOriginalScrollPadding() {
        val padding = PaddingValues(16.dp)
        assertSame(padding, coffeePeekScrollContentPadding(padding, CoffeePeekScreenInsets(0.dp, true)))
    }
}
