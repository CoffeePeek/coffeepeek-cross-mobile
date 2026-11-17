package com.coffeepeek.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.coffee.IosCoffeeDetail
import com.coffeepeek.admin.di.coffee.LocalIosCoffeeDetailController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.auth.IosAuthScreen
import com.coffeepeek.admin.di.auth.LocalIosAuthController
import com.coffeepeek.admin.di.profile.IosGuestProfile
import com.coffeepeek.admin.di.profile.LocalIosGuestProfileController
import com.coffeepeek.admin.di.profile.IosOwnProfile
import com.coffeepeek.admin.di.profile.LocalIosOwnProfileController
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
import com.coffeepeek.admin.di.shop.IosShopCheckInList
import com.coffeepeek.admin.di.shop.LocalIosShopCheckInListController
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
    makeShopCheckInListController: (IosShopCheckInList, String) -> UIViewController,
    makeCoffeeDetailController: (IosCoffeeDetail, String) -> UIViewController,
    makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    makeContentReportController: (IosContentReport, String) -> UIViewController,
    makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    makeSettingsController: (IosSettingsScreen) -> UIViewController,
    makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    makeAuthController: (IosAuthScreen) -> UIViewController,
    makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    makeOwnProfileController: (IosOwnProfile) -> UIViewController,
): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    val controllers = IosControllerFactories(
        makeShopMenuGalleryController,
        makeShopCheckInListController,
        makeCoffeeDetailController,
        makeThemeSettingsController,
        makeShopReportController,
        makeContentReportController,
        makeCollectionsLibraryController,
        makeSettingsController,
        makeBlockedAuthorsController,
        makeAuthController,
        makeGuestProfileController,
        makeOwnProfileController,
    )
    return ComposeUIViewController { controllers.Content(onReady) }
}

private class IosControllerFactories(
    private val makeShopMenuGalleryController: (IosShopMenuGallery, String) -> UIViewController,
    private val makeShopCheckInListController: (IosShopCheckInList, String) -> UIViewController,
    private val makeCoffeeDetailController: (IosCoffeeDetail, String) -> UIViewController,
    private val makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    private val makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    private val makeContentReportController: (IosContentReport, String) -> UIViewController,
    private val makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    private val makeSettingsController: (IosSettingsScreen) -> UIViewController,
    private val makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    private val makeAuthController: (IosAuthScreen) -> UIViewController,
    private val makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    private val makeOwnProfileController: (IosOwnProfile) -> UIViewController,
) {
    @Composable
    fun Content(onReady: () -> Unit) {
        CompositionLocalProvider(
            LocalIosShopMenuGalleryController provides makeShopMenuGalleryController,
            LocalIosShopCheckInListController provides makeShopCheckInListController,
            LocalIosCoffeeDetailController provides makeCoffeeDetailController,
            LocalIosThemeSettingsController provides makeThemeSettingsController,
            LocalIosShopReportController provides makeShopReportController,
            LocalIosContentReportController provides makeContentReportController,
            LocalIosCollectionsLibraryController provides makeCollectionsLibraryController,
            LocalIosSettingsController provides makeSettingsController,
            LocalIosBlockedAuthorsController provides makeBlockedAuthorsController,
            LocalIosAuthController provides makeAuthController,
            LocalIosGuestProfileController provides makeGuestProfileController,
            LocalIosOwnProfileController provides makeOwnProfileController,
        ) {
            App(onReady = onReady)
        }
    }
}
