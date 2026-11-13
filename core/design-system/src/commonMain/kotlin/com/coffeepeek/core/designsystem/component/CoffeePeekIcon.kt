package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Resolves shared interface icons to the platform artwork when available. */
@Composable
fun coffeePeekIconPainter(icon: ImageVector): Painter =
    coffeePeekSystemSymbolPainter(icon) ?: rememberVectorPainter(icon)

@Composable
internal expect fun coffeePeekSystemSymbolPainter(icon: ImageVector): Painter?

@Composable
fun CoffeePeekIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val systemSymbol = coffeePeekSystemSymbolPainter(imageVector)
    if (systemSymbol != null) Icon(systemSymbol, contentDescription, modifier, tint)
    else Icon(imageVector, contentDescription, modifier, tint)
}

@Composable
fun CoffeePeekIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = Icon(painter, contentDescription, modifier, tint)

@Composable
private fun CoffeePeekIconPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoffeePeekIcon(CoffeePeekIcons.Search, null)
            CoffeePeekIcon(CoffeePeekIcons.FavoriteFilled, null, Modifier.size(20.dp))
            CoffeePeekIcon(CoffeePeekIcons.CoffeeBean, null)
        }
    }
}

@Preview @Composable private fun CoffeePeekIconLightPreview() = CoffeePeekIconPreview(false)
@Preview @Composable private fun CoffeePeekIconDarkPreview() = CoffeePeekIconPreview(true)
