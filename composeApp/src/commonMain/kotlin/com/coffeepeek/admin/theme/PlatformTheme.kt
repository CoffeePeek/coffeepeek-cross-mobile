package com.coffeepeek.admin.theme

import androidx.compose.runtime.Composable

/**
 * Applies the app theme to the platform night mode so Android system UI
 * (splash screen, status bar resources, values-night) matches the in-app theme.
 */
expect fun applyPlatformNightMode(mode: ThemeMode)

@Composable
expect fun PlatformSystemBars(darkTheme: Boolean, lightStatusBarIcons: Boolean = darkTheme)

@Composable
internal expect fun platformSystemDarkTheme(): Boolean
