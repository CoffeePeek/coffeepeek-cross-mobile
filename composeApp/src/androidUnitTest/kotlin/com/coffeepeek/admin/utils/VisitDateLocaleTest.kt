package com.coffeepeek.admin.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class VisitDateLocaleTest {
    @Test
    fun selectedLanguageUsesItsMonthCaseWithoutChangingTheUtcDay() {
        assertEquals("1 января 1970", formatVisitDate(0L, "ru"))
        assertEquals("1 студзеня 1970", formatVisitDate(0L, "be"))
        assertEquals("31 снежня 1969", formatVisitDate(-1L, "be"))
    }

    @Test
    fun unsupportedLocaleKeepsTheRussianFallback() {
        assertEquals("1 января 1970", formatVisitDate(0L, "en"))
    }
}
