package com.coffeepeek.admin.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle

/** UIKit controls must follow the app's selected theme, including an override of the system theme. */
internal val ColorScheme.uiKitStyle: UIUserInterfaceStyle
    get() = if (background.luminance() < .5f) UIUserInterfaceStyle.UIUserInterfaceStyleDark
        else UIUserInterfaceStyle.UIUserInterfaceStyleLight

/** Container controllers must release their override when returning to the system theme. */
internal val ThemeMode.uiKitStyle: UIUserInterfaceStyle
    get() = when (this) {
        ThemeMode.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
        ThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        ThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }

internal fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble(),
)

actual fun applyPlatformNightMode(mode: ThemeMode) = Unit

@Composable
actual fun PlatformSystemBars(darkTheme: Boolean, lightStatusBarIcons: Boolean) {
    val controller = LocalUIViewController.current
    val mode by ThemeManager.themeMode.collectAsState()
    SideEffect {
        controller.view.window?.overrideUserInterfaceStyle = mode.uiKitStyle
        controller.overrideUserInterfaceStyle = mode.uiKitStyle
        controller.setNeedsStatusBarAppearanceUpdate()
    }
}

@Composable
internal actual fun platformSystemDarkTheme(): Boolean {
    val observedDark = isSystemInDarkTheme()
    // A screen's traits can still contain the app's previous forced appearance.
    val systemStyle = LocalUIViewController.current.view.window?.windowScene?.traitCollection?.userInterfaceStyle
    return systemDarkTheme(systemStyle, observedDark)
}

internal fun systemDarkTheme(style: UIUserInterfaceStyle?, fallback: Boolean): Boolean = when (style) {
    UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
    UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
    else -> fallback
}
