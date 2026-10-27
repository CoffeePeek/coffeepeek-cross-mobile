package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** Uses the platform's coffee symbol: SF Symbols on iOS and the shared icon on Android. */
@Composable
expect fun CoffeePeekPlatformCoffeeIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
)

@Preview @Composable
private fun CoffeePeekPlatformCoffeeIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekPlatformCoffeeIcon(contentDescription = null)
}

@Preview @Composable
private fun CoffeePeekPlatformCoffeeIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekPlatformCoffeeIcon(contentDescription = null)
}
