package com.coffeepeek.feature.roaster.domain

import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoasterSummaryTest {
    private fun summary(address: RoasterAddress?) = RoasterSummary(address, "R", null, emptyList(), 0, 0, 0)

    @Test fun slugIsTheRouteKeyOnlyWhenPresent() {
        assertEquals("roast", summary(RoasterAddress("roast", "/roasters/roast", 1, false)).slug)
        assertNull(summary(RoasterAddress(" ", "", 0, false)).slug)
        assertNull(summary(null).slug)
    }
}
