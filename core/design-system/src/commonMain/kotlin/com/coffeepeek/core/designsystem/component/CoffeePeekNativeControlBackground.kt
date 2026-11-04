package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import dev.chrisbanes.haze.HazeState

/** Decorative only: Compose owns the foreground, touch handling and accessibility. */
@Composable
internal expect fun CoffeePeekNativeControlBackground(
    modifier: Modifier,
    prominent: Boolean = false,
    tint: Color? = null,
    enabled: Boolean = true,
    pressed: Boolean = false,
    shape: Shape = RoundedCornerShape(percent = 50),
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
)
