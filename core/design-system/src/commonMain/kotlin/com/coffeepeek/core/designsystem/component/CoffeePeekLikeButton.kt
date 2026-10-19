package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_like_add
import com.coffeepeek.core.designsystem.resources.coffeepeek_like_remove
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CoffeePeekLikeButtonStyle { Plain, Glass }

/** Stateless toggle: callers own persistence, auth, haptics, loading and analytics.
 * Container/shape may match a card or photo; the heart's affinity color is fixed.
 */
@Composable
fun CoffeePeekLikeButton(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
    iconSize: Dp = 24.dp,
    overImage: Boolean = false,
    style: CoffeePeekLikeButtonStyle = if (overImage && containerColor == Color.Transparent) {
        CoffeePeekLikeButtonStyle.Glass
    } else {
        CoffeePeekLikeButtonStyle.Plain
    },
) {
    IconToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        enabled = enabled,
        modifier = modifier.coffeePeekLikeBackground(style, shape).clip(shape),
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = if (style == CoffeePeekLikeButtonStyle.Glass) Color.Transparent else containerColor,
            checkedContainerColor = if (style == CoffeePeekLikeButtonStyle.Glass) Color.Transparent else containerColor,
            disabledContainerColor = if (style == CoffeePeekLikeButtonStyle.Glass) Color.Transparent else containerColor,
        ),
    ) {
        CoffeePeekLikeIcon(selected, contentDescription, size = iconSize, enabled = enabled, overImage = overImage)
    }
}

@Composable
internal fun Modifier.coffeePeekLikeBackground(style: CoffeePeekLikeButtonStyle, shape: Shape): Modifier =
    if (style == CoffeePeekLikeButtonStyle.Glass) {
        coffeePeekLiquidGlass(shape, shadowElevation = 0.dp, tintColor = Color.Black.copy(alpha = 0.60f))
    } else this

@Composable
private fun LikeButtonPreviewContent() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekLikeButton(false, stringResource(Res.string.coffeepeek_like_add), {})
        CoffeePeekLikeButton(true, stringResource(Res.string.coffeepeek_like_remove), {})
        CoffeePeekLikeButton(true, stringResource(Res.string.coffeepeek_like_remove), {}, enabled = false)
        CoffeePeekLikeButton(true, stringResource(Res.string.coffeepeek_like_remove), {}, overImage = true)
        CoffeePeekLikeButton(true, stringResource(Res.string.coffeepeek_like_remove), {}, overImage = true, style = CoffeePeekLikeButtonStyle.Plain)
    }
}

@Preview @Composable private fun CoffeePeekLikeButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LikeButtonPreviewContent()
}
@Preview @Composable private fun CoffeePeekLikeButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LikeButtonPreviewContent()
}
