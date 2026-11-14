package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekReduceMotion
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekLikeCount(
    count: Int,
    selected: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val target = CoffeePeekDimens.minimumTouchTarget
    val actionModifier = if (onClick == null) Modifier.semantics { this.selected = selected }
    else Modifier.toggleable(
        value = selected, enabled = enabled, role = Role.Checkbox,
        interactionSource = null, indication = null, onValueChange = { onClick() },
    )
    Row(
        modifier.sizeIn(minWidth = target, minHeight = target).then(actionModifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        CoffeePeekReactionHeart(selected)
        Text(count.toString(), Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CoffeePeekReactionHeart(selected: Boolean) {
    val targetFill = if (selected) 1f else 0f
    val fill = remember { Animatable(targetFill) }
    val scale = remember { Animatable(1f) }
    val reduceMotion = coffeePeekReduceMotion()
    LaunchedEffect(selected, reduceMotion) {
        if (reduceMotion) {
            scale.snapTo(1f)
            fill.snapTo(targetFill)
        } else if (fill.value != targetFill || scale.value != 1f) {
            scale.animateTo(1.24f, tween(95))
            fill.animateTo(targetFill, tween(80))
            scale.animateTo(1f, tween(145))
        }
    }
    Box(Modifier.size(24.dp).graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }) {
        CoffeePeekIcon(CoffeePeekIcons.Favorite, null, Modifier.matchParentSize().graphicsLayer {
            alpha = 1f - fill.value
        }, tint = MaterialTheme.colorScheme.onSurface)
        CoffeePeekIcon(CoffeePeekIcons.FavoriteFilled, null, Modifier.matchParentSize().graphicsLayer {
            alpha = fill.value
        }, tint = CoffeePeekColors.Like)
    }
}

@Composable
private fun CoffeePeekLikeCountPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var selected by remember { mutableStateOf(dark) }
    val count = if (selected) 13 else 12
    CoffeePeekLikeCount(count, selected, "Лайки: $count", onClick = { selected = !selected })
}

@Preview @Composable private fun CoffeePeekLikeCountLightPreview() = CoffeePeekLikeCountPreview(false)
@Preview @Composable private fun CoffeePeekLikeCountDarkPreview() = CoffeePeekLikeCountPreview(true)
