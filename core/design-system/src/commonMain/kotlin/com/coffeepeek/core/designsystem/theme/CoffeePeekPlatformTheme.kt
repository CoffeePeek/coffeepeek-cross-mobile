package com.coffeepeek.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import org.jetbrains.compose.ui.tooling.preview.Preview

internal expect val coffeePeekUsesNativeGlass: Boolean

/** Shared content tokens; platform controls choose their native rendering separately. */
@Composable
fun CoffeePeekPlatformTheme(
    colorScheme: ColorScheme,
    typography: Typography,
    shapes: Shapes,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes, content = content)
}

@Composable
private fun CoffeePeekPlatformThemePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekButton("Continue", {})
}

@Preview @Composable private fun CoffeePeekPlatformThemeLightPreview() = CoffeePeekPlatformThemePreview(false)
@Preview @Composable private fun CoffeePeekPlatformThemeDarkPreview() = CoffeePeekPlatformThemePreview(true)
