package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePeekRoasterCardTest {
    @Test fun aCounterShowsItsNumberAndADashForNoneOrNonsense() {
        assertEquals("7", coffeePeekCountLabel(7))
        assertEquals("1200", coffeePeekCountLabel(1200))
        assertEquals("—", coffeePeekCountLabel(0))
        assertEquals("—", coffeePeekCountLabel(-1))
    }
}
