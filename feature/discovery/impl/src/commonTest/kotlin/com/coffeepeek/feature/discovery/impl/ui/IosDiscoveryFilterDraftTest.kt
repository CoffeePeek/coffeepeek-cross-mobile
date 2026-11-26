package com.coffeepeek.feature.discovery.impl.ui

// Native draft semantics are independent of a platform renderer.

import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosDiscoveryFilterDraftTest {
    @Test
    fun changesStayInTheDraftAndApplyPreservesTheLatestCity() {
        val filters = DiscoveryFilters(cityId = "minsk", openOnly = true, tagIds = setOf("wifi"))
        val draft = DiscoveryFilterDraft(filters)
        draft.toggle("Tags", "wifi")
        draft.toggle("Beans", "arabica")
        draft.price(2)

        assertEquals(setOf("wifi"), filters.tagIds)
        val applied = draft.applyTo(filters.copy(cityId = "brest"))
        assertEquals("brest", applied.cityId)
        assertTrue(applied.openOnly)
        assertEquals(emptySet(), applied.tagIds)
        assertEquals(setOf("arabica"), applied.beanIds)
        assertEquals(PriceLevel.Moderate, applied.priceLevel)
    }

    @Test
    fun resetClearsAllSelectionsAndInvalidSheetActionsDoNotChangeTheDraft() {
        val filters = DiscoveryFilters(cityId = "minsk", openOnly = true, priceLevel = PriceLevel.Expensive)
        val draft = DiscoveryFilterDraft(filters)
        draft.price(-1)
        draft.toggle("unknown", "wifi")
        assertEquals(filters, draft.filters)

        draft.reset()
        assertEquals(DiscoveryFilters(cityId = "minsk"), draft.filters)
    }
}
