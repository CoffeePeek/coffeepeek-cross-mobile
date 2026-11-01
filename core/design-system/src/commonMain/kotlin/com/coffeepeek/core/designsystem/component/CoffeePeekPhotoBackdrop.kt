package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_like_add
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekUsesNativeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Same-window photo capture. Overlay controls are siblings, never part of their own backdrop.
 * Native iOS presenters own the material; no additional Haze layer is captured there.
 * The caller sets the photo bounds. Do not pass this source into a popup or native map surface.
 */
@Composable
fun CoffeePeekPhotoBackdrop(
    modifier: Modifier = Modifier,
    photo: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val source = if (coffeePeekUsesNativeGlass) null else rememberHazeState()
    Box(modifier) {
        Box(Modifier.matchParentSize().then(if (source != null) Modifier.hazeSource(source) else Modifier)) {
            photo()
        }
        CompositionLocalProvider(LocalCoffeePeekGlassHazeState provides source) { overlay() }
    }
}

@Composable
private fun PhotoBackdropPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        CoffeePeekPhotoBackdrop(
            modifier = Modifier.size(200.dp, 100.dp),
            photo = {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                    listOf(Color.Black, Color.White, Color(0xFF61339A), Color.White),
                )))
            },
            overlay = {
                CoffeePeekLikeButton(false, stringResource(Res.string.coffeepeek_like_add), {},
                    modifier = Modifier.align(Alignment.TopEnd), overImage = true)
            },
        )
    }
}

@Preview @Composable private fun CoffeePeekPhotoBackdropLightPreview() = PhotoBackdropPreview(false)
@Preview @Composable private fun CoffeePeekPhotoBackdropDarkPreview() = PhotoBackdropPreview(true)
