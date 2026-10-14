package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryDrink
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HistoryCalendarPresentationTest {
    @Test fun septemberStartsOnTuesdayAndWeeksAreComplete() {
        val cells = historyCalendarCells(HistoryMonth(2026, 9))
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), cells.take(7))
        assertEquals(35, cells.size)
        assertEquals((1..30).toList(), cells.filterNotNull())
    }

    @Test fun leapFebruaryAndEarliestSupportedYearHaveValidMondayFirstGrids() {
        assertEquals(29, historyCalendarCells(HistoryMonth(2024, 2)).filterNotNull().last())
        assertEquals(1, historyCalendarCells(HistoryMonth(1, 1)).first())
    }

    @Test fun streaksNeverConnectEmptyDaysOrWrapOutsideMonth() {
        val month = HistoryMonth(2026, 10)
        val dates = setOf("2026-09-30", "2026-10-01", "2026-10-02", "2026-10-04")
        assertEquals(HistoryDayStreak(false, true), historyDayStreak(month, 1, dates))
        assertEquals(HistoryDayStreak(true, false), historyDayStreak(month, 2, dates))
        assertFalse(historyDayStreak(month, 3, dates).isConnected)
        assertFalse(historyDayStreak(month, 4, dates).isConnected)
    }

    @Test fun russianCountFormsRemainCorrectEvenWithAnEnglishDeviceLocale() {
        assertEquals(listOf(2, 0, 1, 1, 2, 2, 2, 2, 0, 1, 2),
            listOf(0, 1, 2, 4, 5, 11, 12, 14, 21, 22, 111).map(::historyCountForm))
    }

    @Test fun savedDrinkNamesPreferCustomThenActualLocaleWithFallback() {
        val drink = HistoryDrink("cappuccino", null, "Капучино", "Cappuccino")
        assertEquals("Cappuccino", drink.displayName("en"))
        assertEquals("Капучино", drink.displayName("ru"))
        assertEquals("Капучино", drink.copy(nameEn = " ").displayName("en"))
        assertEquals("Авторский кофе", drink.copy(customName = "Авторский кофе").displayName("en"))
    }
}
