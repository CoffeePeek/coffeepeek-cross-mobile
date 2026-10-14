package com.coffeepeek.admin.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
actual fun PlatformSystemBars(darkTheme: Boolean) = Unit
