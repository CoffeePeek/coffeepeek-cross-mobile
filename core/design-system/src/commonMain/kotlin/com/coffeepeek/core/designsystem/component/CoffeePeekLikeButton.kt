package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Glass is a compact photo badge; FloatingGlass matches neighboring glass action buttons. */
enum class CoffeePeekLikeButtonStyle { Plain, Glass, FloatingGlass }

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
        Box(Modifier.size(coffeePeekLikePlateSize(style, iconSize)).coffeePeekLikeBackground(style, shape)
            .then(if (style == CoffeePeekLikeButtonStyle.Plain) Modifier.background(containerColor, shape) else Modifier)
            .clip(shape), contentAlignment = Alignment.Center) {
            if (isLoading) CoffeePeekLoader(label, modifier = Modifier.clearAndSetSemantics {},
                size = 18.dp, strokeWidth = 2.dp)
            else CoffeePeekLikeIcon(selected, null, size = iconSize, enabled = enabled, overImage = overImage)
        }
    }
}

internal fun coffeePeekLikePlateSize(style: CoffeePeekLikeButtonStyle, iconSize: Dp): Dp =
    maxOf(if (style == CoffeePeekLikeButtonStyle.FloatingGlass) CoffeePeekDimens.buttonHeight else 32.dp,
        iconSize + 8.dp)

@Composable
internal fun Modifier.coffeePeekLikeBackground(style: CoffeePeekLikeButtonStyle, shape: Shape): Modifier = when (style) {
    CoffeePeekLikeButtonStyle.Glass ->
        coffeePeekLiquidGlass(shape, shadowElevation = 0.dp, tintColor = Color.Black.copy(alpha = 0.60f))
    CoffeePeekLikeButtonStyle.FloatingGlass -> coffeePeekLiquidGlass(shape)
    CoffeePeekLikeButtonStyle.Plain -> this
}

@Composable
private fun LikeButtonPreviewContent() {
    val add = stringResource(Res.string.coffeepeek_like_add)
    val remove = stringResource(Res.string.coffeepeek_like_remove)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoffeePeekLikeButton(false, add, {})
            CoffeePeekLikeButton(true, remove, {})
            CoffeePeekLikeButton(true, remove, {}, enabled = false)
            CoffeePeekLikeButton(true, remove, {}, overImage = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoffeePeekLikeButton(true, remove, {}, overImage = true, style = CoffeePeekLikeButtonStyle.Plain)
            CoffeePeekLikeButton(false, add, {}, style = CoffeePeekLikeButtonStyle.FloatingGlass)
            CoffeePeekLikeButton(true, remove, {}, style = CoffeePeekLikeButtonStyle.FloatingGlass)
            CoffeePeekLikeButton(true, remove, {}, isLoading = true, loadingDescription = remove)
        }
    }
}

@Preview @Composable private fun CoffeePeekLikeButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LikeButtonPreviewContent()
}
@Preview @Composable private fun CoffeePeekLikeButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LikeButtonPreviewContent()
}
