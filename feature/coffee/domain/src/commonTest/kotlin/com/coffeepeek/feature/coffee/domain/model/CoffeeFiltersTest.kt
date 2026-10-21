package com.coffeepeek.feature.coffee.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoffeeFiltersTest {
    @Test
    fun emptyFiltersHaveNoActiveOptions() {
        assertEquals(0, CoffeeFilters().activeCount)
        assertEquals(1, CoffeeFilters(availableOnly = true).activeCount)
    }

    @Test
    fun toggleCreatesNewDraftWithoutChangingOriginal() {
        val original = CoffeeFilters(availableOnly = true)
        val draft = original.toggle("taste", "tropical")
        assertTrue(original.values.isEmpty())
        assertEquals(setOf("tropical"), draft.values["taste"])
        assertEquals(2, draft.activeCount)
        assertEquals(original, draft.toggle("taste", "tropical"))
    }

    @Test
    fun togglingLastOptionRemovesOnlyItsGroup() {
        val original = CoffeeFilters(mapOf("taste" to setOf("tropical"), "roast" to setOf("light")))
        val next = original.toggle("taste", "tropical")
        assertFalse("taste" in next.values)
        assertEquals(setOf("light"), next.values["roast"])
        assertEquals(1, next.activeCount)
        assertEquals(2, original.activeCount)
    }

    @Test
    fun activeCountCountsOptionsRatherThanGroups() {
        val filters = CoffeeFilters(mapOf("taste" to setOf("tropical", "nut"), "roast" to setOf("light")), true)
        assertEquals(4, filters.activeCount)
        assertEquals(setOf("nut"), filters.toggle("taste", "tropical").values["taste"])
    }
}
