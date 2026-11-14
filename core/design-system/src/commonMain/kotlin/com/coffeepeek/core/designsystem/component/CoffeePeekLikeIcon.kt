package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekReduceMotion
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Shared heart renderer, also for read-only counters and existing glass containers.
 * Selection uses a filled brand-gold heart; an unselected outline is neutral.
 * A decorative icon inside a labelled control must use a null description.
 */
@Composable
fun CoffeePeekLikeIcon(
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    enabled: Boolean = true,
    overImage: Boolean = false,
) = CoffeePeekAnimatedLikeIcon(selected, contentDescription, modifier, size, enabled, overImage)

@Composable
internal fun CoffeePeekAnimatedLikeIcon(
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier,
    size: Dp,
    enabled: Boolean,
    overImage: Boolean,
    isLoading: Boolean = false,
) {
    val targetFill = if (selected) 1f else 0f
    val fill = remember { Animatable(targetFill) }
    val scale = remember { Animatable(1f) }
    val reduceMotion = coffeePeekReduceMotion()
    LaunchedEffect(selected, reduceMotion, isLoading) {
        if (reduceMotion) {
            scale.snapTo(1f)
            fill.snapTo(targetFill)
        } else if (!isLoading && (fill.value != targetFill || scale.value != 1f)) {
            scale.animateTo(1.24f, tween(95))
            fill.animateTo(targetFill, tween(80))
            scale.animateTo(1f, tween(145))
        }
    }
    val outline = coffeePeekLikeColor(false, overImage, MaterialTheme.colorScheme.onSurface)
    Box(modifier.size(size).semantics {
        if (contentDescription != null) this.contentDescription = contentDescription
    }.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        alpha = if (enabled) 1f else 0.38f
    }) {
        CoffeePeekIcon(CoffeePeekIcons.FavoriteFilled, null, Modifier.matchParentSize().graphicsLayer {
            alpha = fill.value
        }, tint = coffeePeekLikeColor(true, overImage, outline))
        // The outline keeps the bright fill legible against plain light surfaces.
        CoffeePeekIcon(CoffeePeekIcons.Favorite, null, Modifier.matchParentSize().graphicsLayer {
            alpha = if (overImage) 1f - fill.value else 1f
        }, tint = outline)
    }
}

internal fun coffeePeekLikeColor(selected: Boolean, overImage: Boolean, content: Color): Color = when {
    selected -> CoffeePeekColors.Like
    overImage -> Color.White
    else -> content
}

@Composable
private fun LikeIconPreviewContent() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CoffeePeekLikeIcon(false, null)
        CoffeePeekLikeIcon(true, null)
        CoffeePeekLikeIcon(true, null, enabled = false)
        Row(Modifier.background(CoffeePeekColors.LikePhotoOverlay).padding(4.dp)) {
            CoffeePeekLikeIcon(true, null, overImage = true)
            CoffeePeekLikeIcon(false, null, overImage = true)
        }
    }
}

@Preview @Composable private fun CoffeePeekLikeIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LikeIconPreviewContent()
}
@Preview @Composable private fun CoffeePeekLikeIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LikeIconPreviewContent()
}
