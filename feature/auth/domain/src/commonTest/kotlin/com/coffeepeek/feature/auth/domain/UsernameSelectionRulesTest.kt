package com.coffeepeek.feature.auth.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsernameSelectionRulesTest {
    @Test fun serverLengthAndCharacterRules() {
        listOf("abc", "  Арсен_01.кофе  ", "a".repeat(30)).forEach { assertTrue(isValidSelectedUsername(it), it) }
        listOf("", "  ", "ab", "a".repeat(31), "1abc", ".abc", "coffee lover", "a-b", "a☕b").forEach {
            assertFalse(isValidSelectedUsername(it), it)
        }
    }
}
