package com.coffeepeek.admin.ui.model

import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import coffeepeek.composeapp.generated.resources.*

import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.core.designsystem.icons.CoffeePeekMapIcon

data class CoffeeFocusOption(
    val id: String,
    val label: org.jetbrains.compose.resources.StringResource,
    val mapIcon: CoffeePeekMapIcon,
)

val COFFEE_FOCUS_OPTIONS: List<CoffeeFocusOption> = listOf(
    CoffeeFocusOption(id = CoffeeShopType.SPECIALTY, label = Res.string.legacy_coffee_focus_discovery_kind_specialty, mapIcon = CoffeePeekMapIcon.Specialty),
    CoffeeFocusOption(id = CoffeeShopType.COFFEE_BAR, label = Res.string.legacy_review_content_ownership_kind_shop, mapIcon = CoffeePeekMapIcon.CoffeeShop),
    CoffeeFocusOption(id = CoffeeShopType.CAFE, label = Res.string.legacy_shop_detail_screen_discovery_kind_cafe, mapIcon = CoffeePeekMapIcon.Cafe),
)
