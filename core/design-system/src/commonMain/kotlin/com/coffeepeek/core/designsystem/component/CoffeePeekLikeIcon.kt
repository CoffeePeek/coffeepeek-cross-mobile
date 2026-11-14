package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * Every selection change pops the heart (a story-like bounce); reduced motion skips it.
 */
@Composable
fun CoffeePeekLikeIcon(
    selected: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    enabled: Boolean = true,
    overImage: Boolean = false,
) {
    val target = coffeePeekLikeColor(selected, overImage, MaterialTheme.colorScheme.onSurface)
    val color by animateColorAsState(target, tween(180), label = "like-color")
    val alpha = if (enabled) 1f else 0.38f
    val motionless = coffeePeekReduceMotion()
    val scale = remember { Animatable(1f) }
    val firstRender = remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (firstRender.value) { firstRender.value = false; return@LaunchedEffect }
        if (motionless) return@LaunchedEffect
        scale.snapTo(0.7f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Box(modifier.graphicsLayer {
        scaleX = scale.value; scaleY = scale.value
    }.size(size)) {
        CoffeePeekIcon(if (selected) CoffeePeekIcons.FavoriteFilled else CoffeePeekIcons.Favorite,
            contentDescription, Modifier.matchParentSize(), tint = color.copy(alpha = alpha))
        if (selected && !overImage) CoffeePeekIcon(CoffeePeekIcons.Favorite, null, Modifier.matchParentSize(),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
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
