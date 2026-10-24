package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.ic_byn_symbol
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The proportions of the currency symbol drawable (446.4 / 360.67). */
private const val BynSymbolHeightPerWidth = 446.4f / 360.67f

/** Decorative currency indicators; the caller supplies price meaning and accessibility text. */
@Composable
fun CoffeePeekBynPriceIndicators(
    count: Int,
    modifier: Modifier = Modifier,
    iconWidth: Dp = 14.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    maxCount: Int = count,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    spacing: Dp = 3.dp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(maxCount.coerceAtLeast(0)) { index ->
            Icon(painterResource(Res.drawable.ic_byn_symbol), contentDescription = null,
                modifier = Modifier.size(width = iconWidth, height = iconWidth * BynSymbolHeightPerWidth),
                tint = if (index < count) tint else inactiveTint)
        }
    }
}

@Preview @Composable private fun CoffeePeekBynPriceIndicatorsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekBynPriceIndicators(count = 2, maxCount = 3)
}

@Preview @Composable private fun CoffeePeekBynPriceIndicatorsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekBynPriceIndicators(count = 2, maxCount = 3)
}
