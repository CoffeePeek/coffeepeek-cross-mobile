package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ScheduleInterval
import com.coffeepeek.domain.model.ShopSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopOpeningLabelTest {
    @Test
    fun findsNextOpeningAcrossBreaksDaysAndWeekBoundary() {
        val monday = ShopSchedule(1, false, listOf(ScheduleInterval("08:00:00", "12:00"), ScheduleInterval("14:00", "20:00")))
        val tuesday = ShopSchedule(2, false, listOf(ScheduleInterval("09:30", "18:00")))
        assertEquals("Откроется сегодня в 08:00", nextShopOpeningLabel(listOf(monday, tuesday), 1, 7 * 60))
        assertEquals("Откроется сегодня в 14:00", nextShopOpeningLabel(listOf(monday, tuesday), 1, 13 * 60))
        assertEquals("Откроется завтра в 09:30", nextShopOpeningLabel(listOf(monday, tuesday), 1, 21 * 60))
        assertEquals("Откроется завтра в 09:30", nextShopOpeningLabel(listOf(monday.copy(isClosed = true), tuesday), 1, 7 * 60))
        assertEquals("Откроется завтра в 08:00", nextShopOpeningLabel(listOf(monday), 0, 22 * 60))
        assertEquals("Откроется в пн в 08:00", nextShopOpeningLabel(listOf(monday), 1, 22 * 60))
        assertEquals("Откроется в вт в 09:30", nextShopOpeningLabel(listOf(tuesday), 0, 22 * 60))
        assertNull(nextShopOpeningLabel(emptyList(), 1, 600))
        assertNull(nextShopOpeningLabel(listOf(monday.copy(isClosed = true)), 1, 600))
        assertNull(nextShopOpeningLabel(listOf(ShopSchedule(1, false, listOf(ScheduleInterval("25:99", "18:00")))), 1, 600))
    }
}
