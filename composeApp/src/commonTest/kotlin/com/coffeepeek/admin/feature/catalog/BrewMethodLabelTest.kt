package com.coffeepeek.admin.feature.catalog

import com.coffeepeek.admin.feature.catalog.api.methodLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class BrewMethodLabelTest {
    @Test
    fun aRecordedMethodKeepsItsCatalogNameAndAnUnknownOneIsShownAsStored() {
        assertEquals("Hario V60", "hario-v60".methodLabel())
        assertEquals("future-method", "future-method".methodLabel())
    }

    @Test
    fun legacyCodesUseTheirRussianFallbackNames() {
        assertEquals("Другое", "other".methodLabel())
        assertEquals("Пуровер", "pour_over".methodLabel())
    }
}
