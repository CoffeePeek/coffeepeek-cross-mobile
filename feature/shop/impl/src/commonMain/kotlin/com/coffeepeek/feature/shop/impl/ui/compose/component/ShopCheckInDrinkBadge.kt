package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekDrinkLabel
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Static saved-drink summary, not a new drink-detail navigation action. */
@Composable
internal fun ShopCheckInDrinkBadge(name: String, modifier: Modifier = Modifier) =
    CoffeePeekDrinkLabel(name, modifier)

@Preview @Composable private fun ShopCheckInDrinkLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInDrinkBadge("Фильтр")
}
@Preview @Composable private fun ShopCheckInDrinkDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInDrinkBadge("Фильтр")
}
