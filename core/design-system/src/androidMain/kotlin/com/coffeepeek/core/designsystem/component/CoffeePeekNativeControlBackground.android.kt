package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import dev.chrisbanes.haze.HazeState

@Composable
internal actual fun CoffeePeekNativeControlBackground(
    modifier: Modifier, prominent: Boolean, tint: Color?, enabled: Boolean, pressed: Boolean,
    shape: Shape, hazeState: HazeState?,
) {
    val color = tint ?: if (prominent) MaterialTheme.colorScheme.primary.copy(alpha = 0.86f) else null
    // Explicit enabled/disabled colors already contain the caller's desired opacity.
    Box(modifier.alpha(if (enabled || tint != null) 1f else 0.45f)
        .coffeePeekLiquidGlass(shape, hazeState, tintColor = color)
        .then(if (pressed) Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape) else Modifier))
}
