package com.coffeepeek.feature.discovery.impl.ui

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopRowTest {
    private val previous = Locale.getDefault()
    @AfterTest fun restore() = Locale.setDefault(previous)

    @Test fun theRatingHasOneDecimalAndADotInEveryLocale() {
        for (locale in listOf(Locale.US, Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("de-DE"))) {
            Locale.setDefault(locale)
            assertEquals("4.0", oneDecimal(4.0))
            assertEquals("4.3", oneDecimal(4.26))
            assertEquals("3.0", oneDecimal(3.04))
            assertEquals("5.0", oneDecimal(4.99))
        }
    }
}
