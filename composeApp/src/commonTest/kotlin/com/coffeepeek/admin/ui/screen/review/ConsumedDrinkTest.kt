package com.coffeepeek.admin.ui.screen.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConsumedDrinkTest {
    @Test fun validatesOtherAndCatalogSelections() {
        assertNull(consumedDrinkIssue(null, null))
        assertNull(consumedDrinkIssue("cappuccino", null))
        assertNotNull(consumedDrinkIssue("cappuccino", "custom"))
        assertNotNull(consumedDrinkIssue("other", "   "))
        assertNull(consumedDrinkIssue("other", " a "))
        assertNull(consumedDrinkIssue("other", "a".repeat(100)))
        assertNotNull(consumedDrinkIssue("other", "a".repeat(101)))
    }
    @Test fun typedIssueUsesPreparedBelarusianCopy() {
        val issue = consumedDrinkIssue("other", " ")
        assertEquals(ConsumedDrinkIssue.CustomNameLength, issue)
        val labels = ConsumedDrinkValidationLabels("Укажыце назву напою", "Назва толькі для іншага напою")
        assertEquals("Укажыце назву напою", issue?.message(labels))
    }
}
