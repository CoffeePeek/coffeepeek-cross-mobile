package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_loading
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekLoader(
    contentDescription: String = stringResource(Res.string.coffeepeek_loading),
    modifier: Modifier = Modifier,
    size: Dp = CoffeePeekDimens.loaderDefault,
    color: Color = CoffeePeekColors.Primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
    progress: Float? = null,
) = CoffeePeekMorphingLoader(contentDescription, modifier, size, color, strokeWidth, progress)

@Composable
private fun CoffeePeekLoaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekLoader() }
}

@Preview @Composable private fun CoffeePeekLoaderLightPreview() = CoffeePeekLoaderPreview(false)
@Preview @Composable private fun CoffeePeekLoaderDarkPreview() = CoffeePeekLoaderPreview(true)

@Preview @Composable private fun CoffeePeekProgressLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { CoffeePeekLoader(progress = .6f) }
}
@Preview @Composable private fun CoffeePeekProgressDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { CoffeePeekLoader(progress = .6f) }
}
