package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CpIcons
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

data class IconBadgeColors(
    val background: Color,
    val icon: Color,
)

object IconBadgePalette {
    val Mint = IconBadgeColors(
        background = Color(0xFFECFAED),
        icon = Color(0xFF5CB66A),
    )
    val Cyan = IconBadgeColors(
        background = Color(0xFFD3EFFA),
        icon = Color(0xFF42AFC2),
    )
    val Sky = IconBadgeColors(
        background = Color(0xFFECF6FF),
        icon = Color(0xFF4AA9EE),
    )
    val Aqua = IconBadgeColors(
        background = Color(0xFFDDFAF8),
        icon = Color(0xFF4DBDB8),
    )
    val Lavender = IconBadgeColors(
        background = Color(0xFFFFF1FF),
        icon = Color(0xFFC96DDD),
    )
    val Gold = IconBadgeColors(
        background = Color(0xFFFEF3C7),
        icon = Color(0xFFCA8A04),
    )
    val Emerald = IconBadgeColors(
        background = Color(0xFFECFDF5),
        icon = Color(0xFF10B981),
    )
    val Blue = IconBadgeColors(
        background = Color(0xFFEFF6FF),
        icon = Color(0xFF3B82F6),
    )
    val Rose = IconBadgeColors(
        background = Color(0xFFFFF1F2),
        icon = Color(0xFFF43F5E),
    )
    val Violet = IconBadgeColors(
        background = Color(0xFFF5F3FF),
        icon = Color(0xFF8B5CF6),
    )
    val BrightCyan = IconBadgeColors(
        background = Color(0xFFECFEFF),
        icon = Color(0xFF06B6D4),
    )
}

@Composable
fun IconBadge(
    icon: ImageVector,
    colors: IconBadgeColors,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    // Derive dark-theme colors from the icon hue instead of a second palette
    // table — the pale light backgrounds read as near-white blocks on a dark surface.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val background = if (isDark) colors.icon.copy(alpha = 0.20f) else colors.background
    val iconTint = if (isDark) lerp(colors.icon, Color.White, 0.30f) else colors.icon
    Box(
        modifier = modifier
            .size(CpDimens.settingsIconContainer)
            .clip(RoundedCornerShape(CpDimens.settingsIconRadius))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(CpDimens.settingsIconSize),
        )
    }
}

@Composable
private fun IconBadgePreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBadge(CpIcons.Settings, IconBadgePalette.Cyan, contentDescription = "Cyan badge")
            IconBadge(CpIcons.Settings, IconBadgePalette.Gold, contentDescription = "Gold badge")
        }
    }
}

@Preview @Composable private fun IconBadgeLightPreview() = IconBadgePreviewContent(false)
@Preview @Composable private fun IconBadgeDarkPreview() = IconBadgePreviewContent(true)
