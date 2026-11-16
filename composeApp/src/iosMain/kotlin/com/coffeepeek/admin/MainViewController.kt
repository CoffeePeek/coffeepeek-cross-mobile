package com.coffeepeek.admin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.shopreport.IosShopReport
import com.coffeepeek.admin.di.shopreport.LocalIosShopReportController
import com.coffeepeek.admin.di.settings.IosThemeSettings
import com.coffeepeek.admin.di.settings.LocalIosThemeSettingsController
import com.coffeepeek.admin.di.shop.IosShopMenuGallery
import com.coffeepeek.admin.di.shop.LocalIosShopMenuGalleryController
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
    makeShopMenuGalleryController: (IosShopMenuGallery, String) -> UIViewController,
    makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalIosShopMenuGalleryController provides makeShopMenuGalleryController,
            LocalIosThemeSettingsController provides makeThemeSettingsController,
            LocalIosShopReportController provides makeShopReportController,
        ) {
            App(onReady = onReady)
        }
    }
}
