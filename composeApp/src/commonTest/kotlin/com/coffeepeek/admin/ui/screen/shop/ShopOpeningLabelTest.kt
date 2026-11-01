package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ScheduleInterval
import com.coffeepeek.domain.model.ShopSchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopOpeningLabelTest {
    private val labels = ShopOpeningLabels("Откроется сегодня в %1\$s", "Откроется завтра в %1\$s", "Откроется в %1\$s в %2\$s", listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб"))

    @Test
    fun findsNextOpeningAcrossBreaksDaysAndWeekBoundary() {
        val monday = ShopSchedule(1, false, listOf(ScheduleInterval("08:00:00", "12:00"), ScheduleInterval("14:00", "20:00")))
        val tuesday = ShopSchedule(2, false, listOf(ScheduleInterval("09:30", "18:00")))
        assertEquals("Откроется сегодня в 08:00", nextShopOpeningLabel(listOf(monday, tuesday), 1, 7 * 60, labels))
        assertEquals("Откроется сегодня в 14:00", nextShopOpeningLabel(listOf(monday, tuesday), 1, 13 * 60, labels))
        assertEquals("Откроется завтра в 09:30", nextShopOpeningLabel(listOf(monday, tuesday), 1, 21 * 60, labels))
        assertEquals("Откроется завтра в 09:30", nextShopOpeningLabel(listOf(monday.copy(isClosed = true), tuesday), 1, 7 * 60, labels))
        assertEquals("Откроется завтра в 08:00", nextShopOpeningLabel(listOf(monday), 0, 22 * 60, labels))
        assertEquals("Откроется в пн в 08:00", nextShopOpeningLabel(listOf(monday), 1, 22 * 60, labels))
        assertEquals("Откроется в вт в 09:30", nextShopOpeningLabel(listOf(tuesday), 0, 22 * 60, labels))
        assertNull(nextShopOpeningLabel(emptyList(), 1, 600, labels))
        assertNull(nextShopOpeningLabel(listOf(monday.copy(isClosed = true)), 1, 600, labels))
        assertNull(nextShopOpeningLabel(listOf(ShopSchedule(1, false, listOf(ScheduleInterval("25:99", "18:00")))), 1, 600, labels))
    }
    @Test
    fun usesPreparedBelarusianWeekdayAndOpeningGrammar() {
        val translated = ShopOpeningLabels("Адкрыецца сёння а %1\$s", "Адкрыецца заўтра а %1\$s", "Адкрыецца ў %1\$s а %2\$s", listOf("нд", "пн", "аў", "ср", "чц", "пт", "сб"))
        val tuesday = ShopSchedule(2, false, listOf(ScheduleInterval("09:30", "18:00")))
        assertEquals("Адкрыецца ў аў а 09:30", nextShopOpeningLabel(listOf(tuesday), 0, 22 * 60, translated))
    }
}
