package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopScheduleStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** Authored for the Android feature completion gate, not yet executed. */
class ShopSchedulePresentationTest {
    @Test fun openShopUsesLastClosingTimeWithoutSeconds() {
        val schedules = listOf(ShopSchedule(1, false,
            listOf(ScheduleInterval("09:00:00", "12:00:00"), ScheduleInterval("13:00:00", "21:30:00"))))
        assertEquals(ShopScheduleStatus.Open("21:30"), shopScheduleStatus(schedules, true, 1, 600))
    }

    @Test fun openWithoutHoursDoesNotInventAClosingTime() {
        assertEquals(ShopScheduleStatus.Open(null), shopScheduleStatus(emptyList(), true, 1, 600))
    }

    @Test fun closedShopCanOpenAgainAfterItsMiddayBreak() {
        val schedules = listOf(ShopSchedule(1, false,
            listOf(ScheduleInterval("13:30:00", "21:00"), ScheduleInterval("09:00", "12:00"))))
        assertEquals(ShopScheduleStatus.Opens(0, 1, "13:30"), shopScheduleStatus(schedules, false, 1, 12 * 60))
    }

    @Test fun nextOpeningSkipsClosedDaysAndWrapsTheWeek() {
        val schedules = listOf(ShopSchedule(6, true), ShopSchedule(0, true),
            ShopSchedule(1, false, listOf(ScheduleInterval("10:00", "21:00"))))
        assertEquals(ShopScheduleStatus.Opens(2, 1, "10:00"), shopScheduleStatus(schedules, false, 6, 700))
    }

    @Test fun sameDayNextWeekIsStillARealNextOpening() {
        val schedules = listOf(ShopSchedule(1, false, listOf(ScheduleInterval("09:00", "21:00"))))
        assertEquals(ShopScheduleStatus.Opens(7, 1, "09:00"), shopScheduleStatus(schedules, false, 1, 22 * 60))
    }

    @Test fun malformedOpeningsAreIgnoredInsteadOfInventingMidnight() {
        val schedules = listOf(ShopSchedule(1, false, listOf(ScheduleInterval("25:00", "21:00"),
            ScheduleInterval("09:75", "21:00"), ScheduleInterval("missing", "21:00"))),
            ShopSchedule(2, false, listOf(ScheduleInterval("08:05", "21:00"))))
        assertEquals(ShopScheduleStatus.Opens(1, 2, "08:05"), shopScheduleStatus(schedules, false, 1, 600))
    }

    @Test fun missingOrInvalidClockKeepsOnlyTheKnownClosedStatus() {
        val schedules = listOf(ShopSchedule(1, true),
            ShopSchedule(2, false, listOf(ScheduleInterval("09:00", "21:00"))))
        assertEquals(ShopScheduleStatus.TodayClosed, shopScheduleStatus(schedules, false, 1, null))
        assertEquals(ShopScheduleStatus.TodayClosed, shopScheduleStatus(schedules, false, 1, 1440))
        assertEquals(ShopScheduleStatus.Closed, shopScheduleStatus(schedules, null, -1, 600))
    }

    @Test fun allClosedWeekKeepsTheDayOffLabel() {
        assertEquals(ShopScheduleStatus.TodayClosed,
            shopScheduleStatus((0..6).map { ShopSchedule(it, true) }, false, 3, 600))
    }
}
