package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekMorphingLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.loader_loading

@Composable
fun CoffeePeekLoader(
    modifier: Modifier = Modifier,
    size: Dp = CpDimens.loaderDefault,
    color: Color = CpColor.Primary,
    strokeWidth: Dp = (size / 16f).coerceAtLeast(2.dp),
    contentDescription: String = stringResource(Res.string.loader_loading),
) = CoffeePeekMorphingLoader(contentDescription, modifier, size, color, strokeWidth)

@Composable
private fun CoffeePeekLegacyLoaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekLoader() }
}

@Preview @Composable private fun CoffeePeekLegacyLoaderLightPreview() = CoffeePeekLegacyLoaderPreview(false)
@Preview @Composable private fun CoffeePeekLegacyLoaderDarkPreview() = CoffeePeekLegacyLoaderPreview(true)
