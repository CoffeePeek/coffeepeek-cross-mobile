package com.coffeepeek.admin.ui.model

import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.core.designsystem.icons.CoffeePeekMapIcon

data class CoffeeFocusOption(
    val id: String,
    val label: String,
    val mapIcon: CoffeePeekMapIcon,
)

val COFFEE_FOCUS_OPTIONS: List<CoffeeFocusOption> = listOf(
    CoffeeFocusOption(id = CoffeeShopType.SPECIALTY, label = "Specialty", mapIcon = CoffeePeekMapIcon.Specialty),
    CoffeeFocusOption(id = CoffeeShopType.COFFEE_BAR, label = "Кофейня", mapIcon = CoffeePeekMapIcon.CoffeeShop),
    CoffeeFocusOption(id = CoffeeShopType.CAFE, label = "Кафе", mapIcon = CoffeePeekMapIcon.Cafe),
)
