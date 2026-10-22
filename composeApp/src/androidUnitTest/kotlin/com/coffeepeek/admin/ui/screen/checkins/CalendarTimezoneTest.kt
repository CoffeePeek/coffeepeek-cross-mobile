package com.coffeepeek.admin.ui.screen.checkins

import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarTimezoneTest {
    @Test fun weekBoundsFollowLocalMidnightAcrossTheDstAndMonthBoundary() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            val week = CalendarWeek("2026-03-29")
            assertEquals("2026-03-23", week.dates.first())
            assertEquals("2026-03-29", week.dates.last())
            assertEquals("2026-03-22T23:00:00Z", week.fromUtc)
            assertEquals("2026-03-29T22:00:00Z", week.toUtc)
            val next = CalendarWeek("2026-04-01")
            assertEquals("2026-03-30", next.dates.first())
            assertEquals("2026-04-05", next.dates.last())
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun monthBoundsUseLocalMidnightIncludingDstTransition() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            val month = CalendarMonth(2026, 3)
            assertEquals(Instant.parse("2026-02-28T23:00:00Z"), Instant.parse(month.fromUtc))
            assertEquals(Instant.parse("2026-03-31T22:00:00Z"), Instant.parse(month.toUtc))
            assertEquals(1, Instant.parse(month.fromUtc).atZone(ZoneId.systemDefault()).dayOfMonth)
        } finally { TimeZone.setDefault(previous) }
    }
}
