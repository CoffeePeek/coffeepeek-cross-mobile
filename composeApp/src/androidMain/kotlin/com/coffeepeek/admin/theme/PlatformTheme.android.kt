package com.coffeepeek.admin.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.coffeepeek.admin.CoffeePeekApplication

private const val PREFS_NAME = "coffeepeek_theme"
private const val KEY_THEME_MODE = "theme_mode"

@Composable
internal actual fun platformSystemDarkTheme(): Boolean = isSystemInDarkTheme()

actual fun applyPlatformNightMode(mode: ThemeMode) {
    persistThemeMode(mode)
    AppCompatDelegate.setDefaultNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        },
    )
}

@Composable
actual fun PlatformSystemBars(darkTheme: Boolean, lightStatusBarIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = !lightStatusBarIcons
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

fun persistThemeMode(mode: ThemeMode) {
    val context = runCatching { CoffeePeekApplication.context }.getOrNull() ?: return
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME_MODE, mode.name)
        .apply()
}

fun readPersistedThemeMode(): ThemeMode? {
    val context = runCatching { CoffeePeekApplication.context }.getOrNull() ?: return null
    val raw = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_THEME_MODE, null)
        ?: return null
    return runCatching { ThemeMode.valueOf(raw) }.getOrNull()
}

fun applyPersistedNightModeEarly() {
    val mode = readPersistedThemeMode() ?: ThemeMode.SYSTEM
    AppCompatDelegate.setDefaultNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        },
    )
}
