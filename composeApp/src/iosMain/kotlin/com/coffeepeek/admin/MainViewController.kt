package com.coffeepeek.admin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.admin.di.collections.LocalIosCollectionsLibraryController
import com.coffeepeek.admin.di.settings.IosThemeSettings
import com.coffeepeek.admin.di.settings.LocalIosThemeSettingsController
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.appLinkScreen
import com.coffeepeek.core.designsystem.component.configureCoffeePeekNativeGlass
import platform.UIKit.UIViewController
import platform.UIKit.UIVisualEffectView

private var koinInitialized = false

fun configureNativeGlass(makeView: () -> UIVisualEffectView) {
    configureCoffeePeekNativeGlass(makeView)
}

fun handleAppLink(link: String): Boolean {
    val screen = appLinkScreen(link) ?: return false
    Navigator.openAppLink(screen)
    return true
}

fun MainViewController(
    onReady: () -> Unit,
    makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalIosThemeSettingsController provides makeThemeSettingsController,
            LocalIosCollectionsLibraryController provides makeCollectionsLibraryController,
        ) {
            App(onReady = onReady)
        }
    }
}
