package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlass
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Decorative material over a same-window Compose backdrop; contains no control or native touch surface.
 * Capture only the content behind this surface with hazeSource, never the surface itself.
 */
@Composable
fun CoffeePeekFrostedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = .3f),
    blurRadius: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val surface = MaterialTheme.colorScheme.surface
    val platformOpaque = coffeePeekFrostedSurfacePlatformOpaque()
    val opaque = (LocalCoffeePeekOpaqueGlassOverride.current ?: LocalCoffeePeekOpaqueGlass.current) || platformOpaque
    val fallback = tint.compositeOver(surface)
    val material = if (!opaque && hazeState != null) Modifier.hazeEffect(hazeState, HazeStyle(
        backgroundColor = surface,
        tint = HazeTint(tint),
        blurRadius = blurRadius,
        noiseFactor = 0f,
        fallbackTint = HazeTint(fallback),
    )) else Modifier.background(fallback)
    Box(modifier.clip(shape).then(material), content = content)
}

@Composable
internal expect fun coffeePeekFrostedSurfacePlatformOpaque(): Boolean

@Composable
private fun FrostedSurfacePreview(dark: Boolean, opaque: Boolean = false) {
    CompositionLocalProvider(LocalCoffeePeekOpaqueGlassOverride provides opaque) {
        CoffeePeekTheme(darkTheme = dark) {
            val backdrop = rememberHazeState()
            Box(Modifier.size(280.dp, 96.dp).background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.matchParentSize().hazeSource(backdrop)) {
                    Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                        listOf(Color(0xFF654F8A), Color(0xFFE6C3A0), Color(0xFF557A72)),
                    )))
                }
                CoffeePeekFrostedSurface(Modifier.align(Alignment.Center).size(248.dp, 32.dp), hazeState = backdrop)
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekFrostedSurfaceLightPreview() = FrostedSurfacePreview(false)
@Preview @Composable private fun CoffeePeekFrostedSurfaceDarkPreview() = FrostedSurfacePreview(true)
@Preview @Composable private fun CoffeePeekFrostedSurfaceOpaqueLightPreview() = FrostedSurfacePreview(false, true)
@Preview @Composable private fun CoffeePeekFrostedSurfaceOpaqueDarkPreview() = FrostedSurfacePreview(true, true)
