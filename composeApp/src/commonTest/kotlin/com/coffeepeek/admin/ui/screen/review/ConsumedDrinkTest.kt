package com.coffeepeek.admin.ui.screen.review

import kotlin.test.assertEquals

import com.coffeepeek.domain.model.consumedDrinkIssue

import com.coffeepeek.domain.model.ConsumedDrinkIssue

import com.coffeepeek.domain.model.validateConsumedDrink
import com.coffeepeek.domain.model.savedDrinkName
import kotlin.test.*

class ConsumedDrinkTest {
    @Test fun validatesOtherAndCatalogSelections() {
        assertNull(validateConsumedDrink(null, null))
        assertNull(validateConsumedDrink("cappuccino", null))
        assertNotNull(validateConsumedDrink("cappuccino", "custom"))
        assertNotNull(validateConsumedDrink("other", "   "))
        assertNull(validateConsumedDrink("other", " a "))
        assertNull(validateConsumedDrink("other", "a".repeat(100)))
        assertNotNull(validateConsumedDrink("other", "a".repeat(101)))
        assertEquals("Старое название", savedDrinkName("Старое название", "Old name", null))
        assertEquals("Old name", savedDrinkName("Старое название", "Old name", null, "en"))
        assertEquals("Старое название", savedDrinkName("Старое название", null, null, "en"))
        assertEquals("Эспрессо-тоник", savedDrinkName("Другой", "Other", "Эспрессо-тоник"))
        assertEquals("Эспрессо-тоник", savedDrinkName("Другой", "Other", "Эспрессо-тоник", "en"))
        assertNull(savedDrinkName(null, null, null, "en"))
    }
    @Test fun typedIssueUsesPreparedBelarusianCopy() {
        val issue = consumedDrinkIssue("other", " ")
        assertEquals(ConsumedDrinkIssue.CustomNameLength, issue)
        val labels = ConsumedDrinkValidationLabels("Укажыце назву напою", "Назва толькі для іншага напою")
        assertEquals("Укажыце назву напою", issue?.message(labels))
    }
}
