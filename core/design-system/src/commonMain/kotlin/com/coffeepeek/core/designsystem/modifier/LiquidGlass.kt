package com.coffeepeek.core.designsystem.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Haze source for glass controls on the current screen. Null → glass falls back to a translucent
 * tint without backdrop blur (e.g. over the native map view, which Haze can't sample).
 */
val LocalGlassHazeState = compositionLocalOf<HazeState?> { null }

/**
 * iOS "Liquid Glass" material: soft shadow, backdrop blur tinted with the surface colour,
 * and a top-lit rim highlight.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    hazeState: HazeState? = LocalGlassHazeState.current,
    shadowElevation: Dp = 6.dp,
): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5f
    val rim = Brush.verticalGradient(
        if (isDark) {
            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.05f))
        } else {
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f))
        },
    )
    val shadowColor = Color.Black.copy(alpha = if (isDark) 0.4f else 0.16f)
    return this
        .shadow(shadowElevation, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = surface,
                        tint = HazeTint(surface.copy(alpha = if (isDark) 0.38f else 0.52f)),
                        blurRadius = 24.dp,
                        noiseFactor = 0f,
                    ),
                )
            } else {
                Modifier.background(surface.copy(alpha = if (isDark) 0.72f else 0.78f))
            },
        )
        .border(0.75.dp, rim, shape)
}

/** Circular glass button (44dp tap target) for navigation / floating controls. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hazeState: HazeState? = LocalGlassHazeState.current,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(CpDimens.buttonHeight)
            .liquidGlass(CircleShape, hazeState),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        ) {
            content()
        }
    }
}

@Composable
private fun GlassIconButtonPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        GlassIconButton({}, contentDescription = "Close", hazeState = null) {
            Icon(CpIcons.Close, contentDescription = null)
        }
    }
}

@Preview @Composable private fun GlassIconButtonLightPreview() = GlassIconButtonPreviewContent(false)
@Preview @Composable private fun GlassIconButtonDarkPreview() = GlassIconButtonPreviewContent(true)
