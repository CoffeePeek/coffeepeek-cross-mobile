package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.drink_mock_cappuccino
import coffeepeek.composeapp.generated.resources.drink_mock_espresso
import coffeepeek.composeapp.generated.resources.drink_mock_matcha
import kotlin.test.Test
import kotlin.test.assertEquals

class MockDrinkPhotoTest {
    @Test
    fun selectsLocalMocksFromCatalogSlugsAndCustomLocalizedNames() {
        assertEquals(Res.drawable.drink_mock_matcha, mockDrinkPhoto("Matcha latte", null))
        assertEquals(Res.drawable.drink_mock_matcha, mockDrinkPhoto("Матча", "other"))
        assertEquals(Res.drawable.drink_mock_espresso, mockDrinkPhoto("Другой кофе", "espresso"))
        assertEquals(Res.drawable.drink_mock_espresso, mockDrinkPhoto("Американо", null))
        assertEquals(Res.drawable.drink_mock_cappuccino, mockDrinkPhoto("Капучино", "cappuccino"))
        assertEquals(Res.drawable.drink_mock_cappuccino, mockDrinkPhoto("Авторский напиток", "other"))
    }
}
