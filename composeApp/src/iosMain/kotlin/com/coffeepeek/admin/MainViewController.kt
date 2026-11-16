package com.coffeepeek.admin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.contentreport.IosContentReport
import com.coffeepeek.admin.di.contentreport.LocalIosContentReportController
import com.coffeepeek.admin.di.shopreport.IosShopReport
import com.coffeepeek.admin.di.shopreport.LocalIosShopReportController
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.admin.di.collections.LocalIosCollectionsLibraryController
import com.coffeepeek.admin.di.settings.IosSettingsScreen
import com.coffeepeek.admin.di.authorblocking.IosBlockedAuthors
import com.coffeepeek.admin.di.authorblocking.LocalIosBlockedAuthorsController
import com.coffeepeek.admin.di.settings.IosThemeSettings
import com.coffeepeek.admin.di.settings.LocalIosSettingsController
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
    makeContentReportController: (IosContentReport, String) -> UIViewController,
    makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    makeSettingsController: (IosSettingsScreen) -> UIViewController,
    makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
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
            LocalIosContentReportController provides makeContentReportController,
            LocalIosCollectionsLibraryController provides makeCollectionsLibraryController,
            LocalIosSettingsController provides makeSettingsController,
            LocalIosBlockedAuthorsController provides makeBlockedAuthorsController,
        ) {
            App(onReady = onReady)
        }
    }
}
