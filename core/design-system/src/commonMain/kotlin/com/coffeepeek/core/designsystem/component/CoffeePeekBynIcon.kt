package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.ic_byn_symbol
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Currency glyph beside a monetary amount, not a price-level indicator. */
@Composable
fun CoffeePeekBynIcon(
    modifier: Modifier = Modifier.size(16.dp),
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
) {
    Icon(painterResource(Res.drawable.ic_byn_symbol), contentDescription, modifier, tint)
}

@Preview @Composable private fun CoffeePeekBynIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekBynIcon()
}
@Preview @Composable private fun CoffeePeekBynIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekBynIcon()
}
