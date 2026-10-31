package com.coffeepeek.core.designsystem.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.theme.coffeePeekUsesNativeGlass
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlass
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Screen-owned backdrop; a missing source uses a dense, readable glass fallback. */
val LocalCoffeePeekGlassHazeState = compositionLocalOf<HazeState?> { null }

enum class CoffeePeekGlassIcon { Back, Add, Edit, Favorite, FavoriteFilled, Share, Close }

interface CoffeePeekNativeGlassPresenter {
    @Composable
    fun IconButton(icon: CoffeePeekGlassIcon, onClick: () -> Unit, contentDescription: String,
        modifier: Modifier, enabled: Boolean, selected: Boolean = false, isLoading: Boolean = false): Boolean
}

val LocalCoffeePeekNativeGlassPresenter = staticCompositionLocalOf<CoffeePeekNativeGlassPresenter?> { null }

/** Android glass approximation: backdrop blur, a translucent tint, soft shadow and a lit rim. */
@Composable
fun Modifier.coffeePeekLiquidGlass(
    shape: Shape,
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
    shadowElevation: Dp = 6.dp,
    tintColor: Color? = null,
): Modifier {
    if (coffeePeekUsesNativeGlass) return this
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    val opaque = LocalCoffeePeekOpaqueGlass.current
    val tint = tintColor ?: surface.copy(alpha = if (dark) 0.64f else 0.72f)
    val fallback = tint.compositeOver(surface.copy(alpha = if (opaque) 1f else 0.98f))
    val rim = Brush.verticalGradient(listOf(
        if (opaque) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else Color.White.copy(alpha = if (dark) 0.26f else 0.9f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (opaque) 0.5f else if (dark) 0.05f else 0.09f),
    ))
    return shadow(shadowElevation, shape, clip = false,
        ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
        .clip(shape)
        .then(if (hazeState != null && !opaque) Modifier.hazeEffect(hazeState, HazeStyle(
            backgroundColor = surface,
            tint = HazeTint(tint),
            blurRadius = 24.dp,
            noiseFactor = 0f,
            fallbackTint = HazeTint(fallback),
        )) else Modifier.background(fallback))
        .border(if (opaque) 1.dp else 0.75.dp, rim, shape)
}

/** Platform navigation/floating action, with a 48dp Compose touch target. */
@Composable
fun CoffeePeekGlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
    nativeIcon: CoffeePeekGlassIcon? = null,
    content: @Composable () -> Unit,
) {
    if (nativeIcon != null && LocalCoffeePeekNativeGlassPresenter.current
            ?.IconButton(nativeIcon, onClick, contentDescription, modifier, enabled) == true) return
    CoffeePeekIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
        modifier = modifier.size(CoffeePeekDimens.glassControlSize).semantics { this.contentDescription = contentDescription },
        hazeState = hazeState,
        content = content,
    )
}

@Composable
private fun CoffeePeekGlassIconButtonPreviewContent(darkTheme: Boolean, opaque: Boolean = false) {
    CompositionLocalProvider(LocalCoffeePeekOpaqueGlassOverride provides opaque) {
        CoffeePeekTheme(darkTheme = darkTheme) {
            Surface {
                CoffeePeekGlassIconButton({}, contentDescription = "Close", hazeState = null) {
                    Icon(CoffeePeekIcons.Close, contentDescription = null)
                }
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekGlassIconButtonLightPreview() = CoffeePeekGlassIconButtonPreviewContent(false)
@Preview @Composable private fun CoffeePeekGlassIconButtonDarkPreview() = CoffeePeekGlassIconButtonPreviewContent(true)
@Preview @Composable private fun CoffeePeekGlassIconButtonOpaqueLightPreview() = CoffeePeekGlassIconButtonPreviewContent(false, true)
@Preview @Composable private fun CoffeePeekGlassIconButtonOpaqueDarkPreview() = CoffeePeekGlassIconButtonPreviewContent(true, true)
