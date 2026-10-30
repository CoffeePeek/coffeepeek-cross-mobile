package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
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
) {
    val color = coffeePeekLikeColor(selected, overImage, MaterialTheme.colorScheme.onSurface)
    Icon(
        imageVector = if (selected) CoffeePeekIcons.FavoriteFilled else CoffeePeekIcons.Favorite,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = color.copy(alpha = if (enabled) 1f else 0.38f),
    )
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
