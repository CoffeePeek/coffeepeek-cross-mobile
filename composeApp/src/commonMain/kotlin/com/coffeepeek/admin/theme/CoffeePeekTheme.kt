package com.coffeepeek.admin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme as DesignSystemTheme

/** Legacy entry point; the shared design system owns all platform appearance. */
@Composable
fun CoffeePeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = DesignSystemTheme(darkTheme = darkTheme, content = content)
