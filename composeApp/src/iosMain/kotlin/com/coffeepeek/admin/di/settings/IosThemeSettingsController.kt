package com.coffeepeek.admin.di.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import platform.UIKit.UIViewController

internal val LocalIosThemeSettingsController =
    staticCompositionLocalOf<(IosThemeSettings, String, () -> Unit) -> UIViewController> {
        error("The iOS app must provide a theme settings controller")
    }

@Composable
internal fun iosThemeSettingsContent(): () -> UIViewController {
    val factory = LocalIosThemeSettingsController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return { factory(IosThemeSettings(AppThemePort()), languageTag, Navigator::popBack) }
}
