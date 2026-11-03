package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass as coreLiquidGlass
import dev.chrisbanes.haze.HazeState

/** Legacy source compatibility; all rendering belongs to core/design-system. */
val LocalGlassHazeState = com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    hazeState: HazeState? = LocalGlassHazeState.current,
    shadowElevation: Dp = 6.dp,
): Modifier = coreLiquidGlass(shape, hazeState, shadowElevation)

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hazeState: HazeState? = LocalGlassHazeState.current,
    content: @Composable () -> Unit,
) = CoffeePeekGlassIconButton(onClick, contentDescription, modifier, enabled, hazeState, content = content)

@Composable
private fun LiquidGlassCompatibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    GlassIconButton({}, "Back") { Icon(CoffeePeekIcons.ChevronLeft, null) }
}

@Preview @Composable private fun LiquidGlassCompatibilityLightPreview() = LiquidGlassCompatibilityPreview(false)
@Preview @Composable private fun LiquidGlassCompatibilityDarkPreview() = LiquidGlassCompatibilityPreview(true)
