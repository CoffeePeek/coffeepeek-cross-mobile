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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import com.coffeepeek.core.designsystem.theme.coffeePeekUsesNativeGlass
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekNativeGlassPresenter
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
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlass
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** PhotoBadge matches photo ratings; glass styles are reserved for floating controls. */
enum class CoffeePeekLikeButtonStyle { Plain, PhotoBadge, Glass, FloatingGlass }

/** Stateless toggle: callers own persistence, auth, loading and analytics.
 * Native presenters may supply platform selection feedback.
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
        CoffeePeekLikeButtonStyle.PhotoBadge
    } else {
        CoffeePeekLikeButtonStyle.Plain
    },
    isLoading: Boolean = false,
    loadingDescription: String? = null,
) {
    val label = if (isLoading) requireNotNull(loadingDescription).also {
        require(it.isNotBlank()) { "Loading requires a localized description" }
    } else contentDescription
    if (style.isGlass && LocalCoffeePeekNativeGlassPresenter.current
            ?.IconButton(if (selected) CoffeePeekGlassIcon.FavoriteFilled else CoffeePeekGlassIcon.Favorite,
                onClick, label, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                enabled && !isLoading, selected, isLoading) == true) return
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .coffeePeekGlassPress(pressed, enabled && !isLoading)
        .toggleable(selected, interactionSource = interactions, indication = null,
            enabled = enabled && !isLoading, role = Role.Checkbox, onValueChange = { onClick() })
        .semantics {
            this.contentDescription = label
            if (isLoading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentAlignment = Alignment.Center) {
        if (style.isGlass && coffeePeekUsesNativeGlass) {
            CoffeePeekNativeControlBackground(Modifier.matchParentSize(), enabled = enabled && !isLoading, pressed = pressed)
        }
        // A compact visual plate is independent of the accessible touch target.
        Box(Modifier.size(coffeePeekLikePlateSize(style, iconSize)).coffeePeekLikeBackground(style, shape)
            .then(if (style == CoffeePeekLikeButtonStyle.Plain) Modifier.background(containerColor, shape) else Modifier)
            .clip(shape), contentAlignment = Alignment.Center) {
            if (isLoading) CoffeePeekLoader(label, modifier = Modifier.clearAndSetSemantics {},
                size = 18.dp, strokeWidth = 2.dp)
            else CoffeePeekLikeIcon(selected, null, size = iconSize, enabled = enabled,
                overImage = coffeePeekLikeUsesPhotoColor(overImage, style, LocalCoffeePeekOpaqueGlass.current))
        }
    }
}

private val CoffeePeekLikeButtonStyle.isGlass: Boolean
    get() = this == CoffeePeekLikeButtonStyle.Glass || this == CoffeePeekLikeButtonStyle.FloatingGlass

internal fun coffeePeekLikeUsesPhotoColor(overImage: Boolean, style: CoffeePeekLikeButtonStyle, opaque: Boolean): Boolean =
    overImage && (style == CoffeePeekLikeButtonStyle.Plain || style == CoffeePeekLikeButtonStyle.PhotoBadge || !opaque)

internal fun coffeePeekLikePlateSize(style: CoffeePeekLikeButtonStyle, iconSize: Dp): Dp =
    maxOf(if (style == CoffeePeekLikeButtonStyle.FloatingGlass) CoffeePeekDimens.glassControlSize else 32.dp,
        iconSize + 8.dp)

@Composable
internal fun Modifier.coffeePeekLikeBackground(style: CoffeePeekLikeButtonStyle, shape: Shape): Modifier {
    if (style == CoffeePeekLikeButtonStyle.PhotoBadge) return background(
        com.coffeepeek.core.designsystem.theme.CoffeePeekColors.PhotoBadgeOverlay, shape)
    if (coffeePeekUsesNativeGlass) return this
    return when (style) {
        CoffeePeekLikeButtonStyle.Glass, CoffeePeekLikeButtonStyle.FloatingGlass -> coffeePeekLiquidGlass(shape)
        CoffeePeekLikeButtonStyle.Plain, CoffeePeekLikeButtonStyle.PhotoBadge -> this
    }
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
            CoffeePeekLikeButton(false, add, {}, overImage = true)
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

@Composable
private fun CoffeePeekLikeButtonOpaquePreview(dark: Boolean) {
    CompositionLocalProvider(LocalCoffeePeekOpaqueGlassOverride provides true) {
        CoffeePeekTheme(darkTheme = dark) { LikeButtonPreviewContent() }
    }
}

@Preview @Composable private fun CoffeePeekLikeButtonOpaqueLightPreview() = CoffeePeekLikeButtonOpaquePreview(false)
@Preview @Composable private fun CoffeePeekLikeButtonOpaqueDarkPreview() = CoffeePeekLikeButtonOpaquePreview(true)
