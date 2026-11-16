package com.coffeepeek.admin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.authorblocking.IosBlockedAuthors
import com.coffeepeek.admin.di.authorblocking.LocalIosBlockedAuthorsController
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
    makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalIosThemeSettingsController provides makeThemeSettingsController,
            LocalIosBlockedAuthorsController provides makeBlockedAuthorsController,
        ) {
            App(onReady = onReady)
        }
    }
}
