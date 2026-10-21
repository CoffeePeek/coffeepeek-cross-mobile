package com.coffeepeek.feature.contributions.domain

import com.coffeepeek.feature.contributions.domain.model.DayHours
import com.coffeepeek.feature.contributions.domain.model.TimeInterval
import com.coffeepeek.feature.contributions.domain.rules.*
import kotlin.test.*

class ScheduleRulesTest {
    @Test fun clockStepsStayInsideTheDay() {
        assertEquals("09:30", shiftClock("09:00", 30))
        assertEquals("00:00", shiftClock("00:10", -30))
        assertEquals("23:59", shiftClock("23:50", 30))
        assertEquals("09:00", shiftClock("broken", 0))
    }

    @Test fun unifiedWeekListsMondayFirstAndLeavesClosedDaysEmpty() {
        val week = unifiedWeek("08:00", "20:30", closedDays = setOf(0, 3))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 0), week.map { it.day })
        assertEquals(listOf(3, 0), week.filter { it.closed }.map { it.day })
        assertTrue(week.filter { it.closed }.all { it.intervals.isEmpty() })
        assertEquals(listOf(TimeInterval("08:00", "20:30")), week.first().intervals)
    }

    @Test fun perDayWeekKeepsEachDayAndSortsThemMondayFirst() {
        val week = perDayWeek(listOf(
            DayInput(0, closed = true, open = "09:00", close = "21:00"),
            DayInput(5, closed = false, open = "09:00", close = "23:59"),
            DayInput(1, closed = false, open = "10:00", close = "22:00"),
        ))
        assertEquals(listOf(1, 5, 0), week.map { it.day })
        assertEquals(listOf(TimeInterval("09:00", "23:59")), week[1].intervals)
        assertTrue(week[2].closed && week[2].intervals.isEmpty())
    }

    @Test fun shiftingMovesAnIntervalAcrossMidnightToTheNeighbouringDay() {
        val local = listOf(DayHours(1, closed = false, intervals = listOf(TimeInterval("01:00", "05:00"))))
        val utc = shiftWeek(local, -180)
        assertEquals(7, utc.size)
        assertEquals(listOf(TimeInterval("22:00", "02:00")), utc.single { it.day == 0 }.intervals)
        assertTrue(utc.single { it.day == 1 }.closed)
        assertEquals(local.single(), shiftWeek(utc, 180).single { it.day == 1 })
    }

    @Test fun shiftingAWholeWeekKeepsEveryDayAndClosedDaysStayClosed() {
        val week = unifiedWeek("09:00", "18:00", closedDays = setOf(0))
        val shifted = shiftWeek(week, -120)
        assertEquals((0..6).toList(), shifted.map { it.day })
        assertTrue(shifted.single { it.day == 0 }.closed)
        assertEquals(listOf(TimeInterval("07:00", "16:00")), shifted.single { it.day == 3 }.intervals)
        assertEquals(emptyList(), shiftWeek(emptyList(), 60))
    }

    @Test fun anUnreadableIntervalIsSentAsItWasWithoutMovingDays() {
        val shifted = shiftWeek(listOf(DayHours(2, closed = false, intervals = listOf(TimeInterval("bad", "25:00")))), -60)
        assertEquals(listOf(TimeInterval("bad", "25:00")), shifted.single { it.day == 2 }.intervals)
    }
}
