package com.coffeepeek.feature.coffee.impl.ui.data

import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeePriceFormattingTest {
    @Test fun precisionRegressionsFromTheRetiredAppScreenRemainCovered() {
        assertEquals("45", formatCoffeePrice(45.0))
        assertEquals("18.7", formatCoffeePrice(18.7))
        assertEquals("22.99", formatCoffeePrice(22.99))
        assertEquals("0.05", formatCoffeePrice(0.05))
    }

    @Test fun integersDoNotGainFractionDigits() {
        assertEquals("0", formatCoffeePrice(0.0))
        assertEquals("24", formatCoffeePrice(24.0))
    }
    @Test fun decimalPlacesMatchExistingCatalogue() {
        assertEquals("24.5", formatCoffeePrice(24.5))
        assertEquals("24.05", formatCoffeePrice(24.05))
        assertEquals("24.56", formatCoffeePrice(24.56))
    }
    @Test fun pricesRoundToCentsBeforeFormatting() {
        assertEquals("24.57", formatCoffeePrice(24.567))
        assertEquals("25", formatCoffeePrice(24.999))
    }
}
