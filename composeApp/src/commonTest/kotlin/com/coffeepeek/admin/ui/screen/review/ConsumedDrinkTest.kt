package com.coffeepeek.admin.ui.screen.review

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
}
