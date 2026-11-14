package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons

@Composable
actual fun CoffeePeekPlatformCoffeeIcon(contentDescription: String?, modifier: Modifier, tint: Color) {
    Icon(CoffeePeekIcons.Coffee, contentDescription, modifier, tint)
}
