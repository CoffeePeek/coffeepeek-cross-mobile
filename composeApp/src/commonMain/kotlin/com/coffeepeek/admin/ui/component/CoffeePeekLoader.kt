package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekMorphingLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.loader_loading

@Composable
fun CoffeePeekLoader(
    modifier: Modifier = Modifier,
    size: Dp = CoffeePeekDimens.loaderDefault,
    color: Color = CoffeePeekColors.Primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
    contentDescription: String = stringResource(Res.string.loader_loading),
    progress: Float? = null,
) = CoffeePeekMorphingLoader(contentDescription, modifier, size, color, strokeWidth, progress)

@Composable
private fun CoffeePeekLoaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekLoader() }
}

@Preview @Composable private fun CoffeePeekLoaderLightPreview() = CoffeePeekLoaderPreview(false)
@Preview @Composable private fun CoffeePeekLoaderDarkPreview() = CoffeePeekLoaderPreview(true)
