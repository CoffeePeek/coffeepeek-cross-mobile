package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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
    isLoading: Boolean = false,
    loadingDescription: String? = null,
) {
    val label = if (isLoading) requireNotNull(loadingDescription).also {
        require(it.isNotBlank()) { "Loading requires a localized description" }
    } else contentDescription
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .toggleable(selected, interactionSource = remember { MutableInteractionSource() }, indication = null,
            enabled = enabled && !isLoading, role = Role.Checkbox, onValueChange = { onClick() })
        .semantics {
            this.contentDescription = label
            if (isLoading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentAlignment = Alignment.Center) {
        // A compact visual plate is independent of the accessible touch target.
        Box(Modifier.size(maxOf(32.dp, iconSize + 8.dp)).coffeePeekLikeBackground(style, shape)
            .then(if (style == CoffeePeekLikeButtonStyle.Plain) Modifier.background(containerColor, shape) else Modifier)
            .clip(shape), contentAlignment = Alignment.Center) {
            if (isLoading) CoffeePeekLoader(label, modifier = Modifier.clearAndSetSemantics {},
                size = 18.dp, strokeWidth = 2.dp)
            else CoffeePeekLikeIcon(selected, null, size = iconSize, enabled = enabled, overImage = overImage)
        }
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
        CoffeePeekLikeButton(true, stringResource(Res.string.coffeepeek_like_remove), {}, isLoading = true,
            loadingDescription = stringResource(Res.string.coffeepeek_like_remove))
    }
}

@Preview @Composable private fun CoffeePeekLikeButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LikeButtonPreviewContent()
}
@Preview @Composable private fun CoffeePeekLikeButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LikeButtonPreviewContent()
}
