package com.coffeepeek.admin.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTimeFormattingTest {
    @Test
    fun fractionalBackendTimestampsRenderAsLocalDateAndTime() {
        val expected = utcIsoToLocalDateTime("2026-10-14T20:37:42Z")
        assertTrue(Regex("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}").matches(expected))
        assertEquals(expected, utcIsoToLocalDateTime("2026-10-14T20:37:42.123Z"))
        assertEquals(expected, utcIsoToLocalDateTime("2026-10-14T20:37:42.1234567Z"))
    }

    @Test
    fun explicitOffsetAndUtcRepresentationsDisplayTheSameVisitTime() {
        assertEquals(utcIsoToLocalDateTime("2026-10-14T20:37:42.123Z"),
            utcIsoToLocalDateTime("2026-10-14T23:37:42.123+03:00"))
    }
}
