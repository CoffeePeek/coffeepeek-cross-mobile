package com.coffeepeek.core.designsystem

import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePeekPluralTest {
    private fun form(count: Int) = coffeePeekPluralForm(count, "пачка", "пачки", "пачек")

    @Test fun followsRussianRulesWhateverTheDeviceLanguage() {
        mapOf(
            0 to "пачек", 1 to "пачка", 2 to "пачки", 4 to "пачки", 5 to "пачек", 11 to "пачек", 12 to "пачек",
            14 to "пачек", 20 to "пачек", 21 to "пачка", 22 to "пачки", 25 to "пачек", 31 to "пачка",
            101 to "пачка", 111 to "пачек", 112 to "пачек", 1004 to "пачки", -3 to "пачки",
        ).forEach { (count, expected) -> assertEquals(expected, form(count), "count $count") }
    }
}
