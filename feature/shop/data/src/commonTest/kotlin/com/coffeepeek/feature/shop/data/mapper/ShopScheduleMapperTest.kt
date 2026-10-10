package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopScheduleDto
import com.coffeepeek.feature.shop.data.backend.ShopScheduleIntervalDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopScheduleMapperTest {
    @Test fun negativeOffsetMovesMondayOpeningIntoSunday() {
        val schedules = listOf(
            ShopScheduleDto(
                dayOfWeek = JsonPrimitive("monday"),
                intervals = listOf(ShopScheduleIntervalDto("01:00", "02:00")),
            ),
        ).toLocalSchedules(-180)

        assertEquals("22:00", schedules[0].intervals.single().openTime)
        assertEquals("23:00", schedules[0].intervals.single().closeTime)
        assertTrue(schedules[1].isClosed)
    }

    @Test fun emptyScheduleStaysAbsentInsteadOfInventingSevenClosedDays() {
        assertTrue(emptyList<ShopScheduleDto>().toLocalSchedules(180).isEmpty())
    }

    @Test fun malformedTimeKeepsOriginalValueAsLegacyConverterDoes() {
        val schedules = listOf(
            ShopScheduleDto(
                dayOfWeek = JsonPrimitive(4),
                intervals = listOf(ShopScheduleIntervalDto("unknown", "17:00")),
            ),
        ).toLocalSchedules(0)
        assertEquals("unknown", schedules[4].intervals.single().openTime)
    }
}
