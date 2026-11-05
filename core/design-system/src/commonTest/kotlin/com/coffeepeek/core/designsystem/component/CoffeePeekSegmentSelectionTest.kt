package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure native-callback policy; no Compose rendering or UIKit execution. */
class CoffeePeekSegmentSelectionTest {
    @Test fun disabledControlIgnoresALateCallback() {
        val changes = mutableListOf<String>()
        coffeePeekSelectSegment(listOf("250g", "1000g"), 1, false, changes::add)
        assertTrue(changes.isEmpty())
    }

    @Test fun removedOrInvalidSegmentDoesNotSelectAnotherOption() {
        val changes = mutableListOf<String>()
        for (index in listOf(-1, 1, Int.MAX_VALUE)) {
            coffeePeekSelectSegment(listOf("250g"), index, true, changes::add)
        }
        assertTrue(changes.isEmpty())
    }

    @Test fun validIndexResolvesAgainstTheCurrentOptions() {
        val changes = mutableListOf<String>()
        coffeePeekSelectSegment(listOf("new-250g", "new-1000g"), 1, true, changes::add)
        assertEquals(listOf("new-1000g"), changes)
    }
}
