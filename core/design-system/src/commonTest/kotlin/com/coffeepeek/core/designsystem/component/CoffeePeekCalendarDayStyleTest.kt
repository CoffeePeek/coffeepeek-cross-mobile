package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoffeePeekCalendarDayStyleTest {
    @Test fun photosReplaceDateInEverySelectionAndTodayState() {
        for (today in listOf(false, true)) for (selected in listOf(false, true)) {
            assertFalse(coffeePeekCalendarDayStyle(today, false, true, selected).showDate)
            assertTrue(coffeePeekCalendarDayStyle(today, false, false, selected).showDate)
        }
    }

    @Test fun selectingTodayKeepsItsYellowNumberInsteadOfAnInvertedFill() {
        val today = coffeePeekCalendarDayStyle(true, false, false, true)
        assertEquals(CoffeePeekCalendarDayTone.Accent, today.tone)
        assertTrue(today.outlined)
        assertFalse(today.selectedFill)
    }

    @Test fun todayPhotoHasAVisibleOutlineWithoutDateOverlay() {
        val today = coffeePeekCalendarDayStyle(true, false, true)
        assertTrue(today.outlined)
        assertFalse(today.showDate)
        assertFalse(today.selectedFill)
    }

    @Test fun ordinaryDaysHaveNoTileOrOutlineAndFutureDatesAreMuted() {
        val regular = coffeePeekCalendarDayStyle(false, false, false)
        assertFalse(regular.outlined)
        assertFalse(regular.selectedFill)
        assertEquals(CoffeePeekCalendarDayTone.Content, regular.tone)
        assertEquals(CoffeePeekCalendarDayTone.Muted, coffeePeekCalendarDayStyle(false, true, false).tone)
    }

    @Test fun selectedDateAndSelectedPhotoRetainDistinctSelectionAffordances() {
        val date = coffeePeekCalendarDayStyle(false, false, false, true)
        val photo = coffeePeekCalendarDayStyle(false, false, true, true)
        assertTrue(date.selectedFill)
        assertEquals(CoffeePeekCalendarDayTone.OnAccent, date.tone)
        assertFalse(photo.selectedFill)
        assertTrue(photo.outlined)
    }
}
