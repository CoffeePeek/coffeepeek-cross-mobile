package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.coffeepeek.core.designsystem.theme.coffeePeekUsesNativeGlass

@Composable
internal fun Modifier.coffeePeekGlassPress(pressed: Boolean, enabled: Boolean): Modifier {
    if (coffeePeekUsesNativeGlass) return this
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(140), label = "glass-press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
