package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.icons.CoffeePeekMapIcon
import com.coffeepeek.core.designsystem.icons.coffeePeekMapIconPainter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

data class CoffeePeekIconBadgeColors(
    val background: Color,
    val icon: Color,
)

object CoffeePeekIconBadgePalette {
    val Mint = CoffeePeekIconBadgeColors(
        background = Color(0xFFECFAED),
        icon = Color(0xFF287D3B),
    )
    val Cyan = CoffeePeekIconBadgeColors(
        background = Color(0xFFD3EFFA),
        icon = Color(0xFF247A8B),
    )
    val Sky = CoffeePeekIconBadgeColors(
        background = Color(0xFFECF6FF),
        icon = Color(0xFF246FA3),
    )
    val Aqua = CoffeePeekIconBadgeColors(
        background = Color(0xFFDDFAF8),
        icon = Color(0xFF257D78),
    )
    val Gold = CoffeePeekIconBadgeColors(
        background = Color(0xFFFEF3C7),
        icon = Color(0xFF8A6300),
    )
    val Emerald = CoffeePeekIconBadgeColors(
        background = Color(0xFFECFDF5),
        icon = Color(0xFF087F5B),
    )
    val Rose = CoffeePeekIconBadgeColors(
        background = Color(0xFFFFF1F2),
        icon = Color(0xFFBE123C),
    )
    val Violet = CoffeePeekIconBadgeColors(
        background = Color(0xFFF5F3FF),
        icon = Color(0xFF7C3AED),
    )
}

@Composable
fun CoffeePeekIconBadge(
    icon: ImageVector,
    colors: CoffeePeekIconBadgeColors,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    CoffeePeekIconBadge(coffeePeekIconPainter(icon), colors, modifier, contentDescription)
}

@Composable
fun CoffeePeekIconBadge(
    painter: Painter,
    colors: CoffeePeekIconBadgeColors,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    // Derive dark-theme colors from the icon hue instead of a second palette
    // table — the pale light backgrounds read as near-white blocks on a dark surface.
    val palette = coffeePeekIconBadgeColors(colors, MaterialTheme.colorScheme.surface)
    Box(
        modifier = modifier
            .size(CoffeePeekDimens.settingsIconContainer)
            .clip(RoundedCornerShape(CoffeePeekDimens.settingsIconRadius))
            .background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        CoffeePeekIcon(
            painter = painter,
            contentDescription = contentDescription,
            tint = palette.icon,
            modifier = Modifier.size(CoffeePeekDimens.settingsIconSize),
        )
    }
}

internal fun coffeePeekIconBadgeColors(colors: CoffeePeekIconBadgeColors, surface: Color): CoffeePeekIconBadgeColors =
    if (surface.luminance() < 0.5f) CoffeePeekIconBadgeColors(
        colors.icon.copy(alpha = 0.20f), lerp(colors.icon, Color.White, 0.35f),
    ) else colors

@Composable
private fun CoffeePeekIconBadgePreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoffeePeekIconBadge(CoffeePeekIcons.Settings, CoffeePeekIconBadgePalette.Cyan, contentDescription = "Cyan badge")
            CoffeePeekIconBadge(coffeePeekMapIconPainter(CoffeePeekMapIcon.CoffeeShop), CoffeePeekIconBadgePalette.Gold)
        }
    }
}

@Preview @Composable private fun CoffeePeekIconBadgeLightPreview() = CoffeePeekIconBadgePreviewContent(false)
@Preview @Composable private fun CoffeePeekIconBadgeDarkPreview() = CoffeePeekIconBadgePreviewContent(true)
