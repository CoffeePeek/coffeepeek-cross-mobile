package com.coffeepeek.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/** Compose resolves this to the iOS system font, including script-specific fallback. */
actual val CoffeePeekFontFamily: FontFamily
    @Composable get() = FontFamily.Default
