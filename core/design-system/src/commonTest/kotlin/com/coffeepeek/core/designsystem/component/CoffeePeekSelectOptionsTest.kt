package com.coffeepeek.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertFailsWith

class CoffeePeekSelectOptionsTest {
    private fun option(id: String, selected: Boolean = false) = CoffeePeekSelectOption(id, id, selected) { }

    @Test fun permitsEmptyUnselectedAndSingleSelectedLists() {
        coffeePeekValidateSelectOptions(emptyList())
        coffeePeekValidateSelectOptions(listOf(option("none"), option("city")))
        coffeePeekValidateSelectOptions(listOf(option("none", true), option("city")))
    }

    @Test fun rejectsAmbiguousNativeMenuIdentities() {
        assertFailsWith<IllegalArgumentException> {
            coffeePeekValidateSelectOptions(listOf(option("city"), option("city")))
        }
    }

    @Test fun rejectsMultipleSelections() {
        assertFailsWith<IllegalArgumentException> {
            coffeePeekValidateSelectOptions(listOf(option("none", true), option("city", true)))
        }
    }
}
