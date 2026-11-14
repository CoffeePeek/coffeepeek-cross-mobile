package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.coffeepeek.core.designsystem.icons.CoffeePeekMapIcon
import com.coffeepeek.core.designsystem.icons.coffeePeekMapIconPainter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekCoffeeIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = CoffeePeekIcon(coffeePeekMapIconPainter(CoffeePeekMapIcon.CoffeeShop), contentDescription, modifier, tint)

@Preview @Composable
private fun CoffeePeekCoffeeIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekCoffeeIcon(contentDescription = null)
}

@Preview @Composable
private fun CoffeePeekCoffeeIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekCoffeeIcon(contentDescription = null)
}
