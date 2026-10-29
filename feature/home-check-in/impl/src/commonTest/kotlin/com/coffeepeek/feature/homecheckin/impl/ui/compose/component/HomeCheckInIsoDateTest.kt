package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeCheckInIsoDateTest {
    @Test fun civilDaysMatchTheCalendar() {
        assertEquals(0L, isoEpochDay("1970-01-01"))
        assertEquals(-1L, isoEpochDay("1969-12-31"))
        assertEquals(11_016L, isoEpochDay("2000-02-29"))
        assertEquals(20_755L, isoEpochDay("2026-10-29"))
    }

    @Test fun epochDaysRoundTripThroughIsoDates() {
        for (day in listOf(-719_528L, -1L, 0L, 59L, 11_016L, 20_755L, 2_932_896L)) assertEquals(day, isoEpochDay(isoDate(day)))
        assertEquals("2026-10-29", isoDate(20_755L))
    }

    @Test fun notADateIsNull() {
        for (value in listOf("", "2026-13-01", "2026-02-29", "2026-1-01", "29.10.2026", "2026-10-32")) assertNull(isoEpochDay(value))
    }
}
