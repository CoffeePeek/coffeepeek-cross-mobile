package com.coffeepeek.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.di.coffee.IosCoffeeDetail
import com.coffeepeek.admin.di.coffee.LocalIosCoffeeDetailController
import com.coffeepeek.admin.di.discovery.IosDiscoveryFilters
import com.coffeepeek.admin.di.discovery.LocalIosDiscoveryFilterController
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.notifications.IosNotificationInbox
import com.coffeepeek.admin.di.notifications.LocalIosNotificationInboxController
import com.coffeepeek.admin.di.auth.IosAuthScreen
import com.coffeepeek.admin.di.auth.LocalIosAuthController
import com.coffeepeek.admin.di.homecheckin.IosCheckInCreatePicker
import com.coffeepeek.admin.di.homecheckin.LocalCheckInCreatePicker
import com.coffeepeek.admin.di.homecheckin.iosCheckInCreatePickerContent
import com.coffeepeek.admin.di.profile.IosGuestProfile
import com.coffeepeek.admin.di.profile.LocalIosGuestProfileController
import com.coffeepeek.admin.di.profile.IosOwnProfile
import com.coffeepeek.admin.di.profile.LocalIosOwnProfileController
import com.coffeepeek.admin.di.contributions.IosContributionsRoute
import com.coffeepeek.admin.di.contributions.LocalIosContributionsController
import com.coffeepeek.admin.di.profile.IosPublicProfile
import com.coffeepeek.admin.di.profile.LocalIosPublicProfileController
import com.coffeepeek.admin.di.contentreport.IosContentReport
import com.coffeepeek.admin.di.contentreport.LocalIosContentReportController
import com.coffeepeek.admin.di.shopreport.IosShopReport
import com.coffeepeek.admin.di.shopreport.LocalIosShopReportController
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.admin.di.collections.IosSavedPublicCollection
import com.coffeepeek.admin.di.collections.LocalIosCollectionsLibraryController
import com.coffeepeek.admin.di.collections.LocalIosSavedPublicCollectionController
import com.coffeepeek.admin.di.settings.IosSettingsScreen
import com.coffeepeek.admin.di.authorblocking.IosBlockedAuthors
import com.coffeepeek.admin.di.authorblocking.LocalIosBlockedAuthorsController
import com.coffeepeek.admin.di.shelf.IosShelfArchive
import com.coffeepeek.admin.di.shelf.LocalIosShelfArchiveController
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

fun configureNativeCheckInDetails(factory: (com.coffeepeek.admin.di.community.IosCheckInDetail) -> UIViewController) =
    com.coffeepeek.admin.di.community.configureNativeCheckInDetails(factory)

fun configureNativeCommunityFeed(factory: (com.coffeepeek.admin.di.community.IosCommunityFeed) -> UIViewController) =
    com.coffeepeek.admin.di.community.configureNativeCommunityFeed(factory)

fun configureNativeCommunityCards(factory: (com.coffeepeek.admin.di.community.IosCommunityCard) -> UIViewController) =
    com.coffeepeek.admin.di.community.configureNativeCommunityCards(factory)

fun configureNativeCheckInEditors(
    home: (com.coffeepeek.admin.di.homecheckin.IosHomeCheckInEditor) -> UIViewController,
    shop: (com.coffeepeek.admin.di.shop.IosShopCheckInEditor) -> UIViewController,
) = com.coffeepeek.admin.di.homecheckin.configureNativeCheckInEditors(home, shop)

fun configureNativeEditProfile(factory: (com.coffeepeek.admin.di.profile.IosEditProfile) -> UIViewController) {
    com.coffeepeek.admin.di.profile.configureNativeEditProfile(factory)
}

fun configureNativeGlobalDialogs(factory: (com.coffeepeek.admin.di.system.IosGlobalDialog) -> UIViewController) {
    com.coffeepeek.admin.ui.dialogs.configureNativeGlobalDialogs(factory)
}

fun configureNativeRootModals(factory: (com.coffeepeek.admin.di.system.IosRootModal) -> UIViewController) {
    com.coffeepeek.admin.di.system.configureNativeRootModals(factory)
}

fun configureNativeOwnership(factory: (com.coffeepeek.admin.di.ownership.IosOwnershipScreen) -> UIViewController,
    actions: (com.coffeepeek.admin.di.ownership.IosOwnershipActions) -> UIViewController) {
    com.coffeepeek.admin.di.ownership.configureNativeOwnership(factory)
    com.coffeepeek.admin.di.ownership.configureNativeOwnershipActions(actions)
}

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
    makeDiscoveryFilterController: (IosDiscoveryFilters) -> UIViewController,
    makeShopMenuGalleryController: (IosShopMenuGallery, String) -> UIViewController,
    makeShopCheckInListController: (IosShopCheckInList, String) -> UIViewController,
    makeCoffeeDetailController: (IosCoffeeDetail, String) -> UIViewController,
    makeShelfArchiveController: (IosShelfArchive, String) -> UIViewController,
    makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    makeContributionsController: (IosContributionsRoute, String) -> UIViewController,
    makeContentReportController: (IosContentReport, String) -> UIViewController,
    makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    makeSavedPublicCollectionController: (IosSavedPublicCollection, String) -> UIViewController,
    makeSettingsController: (IosSettingsScreen) -> UIViewController,
    makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    makeAuthController: (IosAuthScreen) -> UIViewController,
    makeCheckInCreatePickerController: (IosCheckInCreatePicker) -> UIViewController,
    makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    makeOwnProfileController: (IosOwnProfile) -> UIViewController,
    makePublicProfileController: (IosPublicProfile) -> UIViewController,
    makeNotificationInboxController: (IosNotificationInbox, String) -> UIViewController,
): UIViewController = IosControllerFactories(
    makeDiscoveryFilterController,
    makeShopMenuGalleryController,
    makeShopCheckInListController,
    makeCoffeeDetailController,
    makeShelfArchiveController,
    makeThemeSettingsController,
    makeShopReportController,
    makeContributionsController,
    makeContentReportController,
    makeCollectionsLibraryController,
    makeSavedPublicCollectionController,
    makeSettingsController,
    makeBlockedAuthorsController,
    makeAuthController,
    makeCheckInCreatePickerController,
    makeGuestProfileController,
    makeOwnProfileController,
    makePublicProfileController,
    makeNotificationInboxController,
).createController(onReady)

private fun IosControllerFactories.createController(onReady: () -> Unit): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return ComposeUIViewController { Content(onReady) }
}

private class IosControllerFactories(
    private val makeDiscoveryFilterController: (IosDiscoveryFilters) -> UIViewController,
    private val makeShopMenuGalleryController: (IosShopMenuGallery, String) -> UIViewController,
    private val makeShopCheckInListController: (IosShopCheckInList, String) -> UIViewController,
    private val makeCoffeeDetailController: (IosCoffeeDetail, String) -> UIViewController,
    private val makeShelfArchiveController: (IosShelfArchive, String) -> UIViewController,
    private val makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    private val makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    private val makeContributionsController: (IosContributionsRoute, String) -> UIViewController,
    private val makeContentReportController: (IosContentReport, String) -> UIViewController,
    private val makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    private val makeSavedPublicCollectionController: (IosSavedPublicCollection, String) -> UIViewController,
    private val makeSettingsController: (IosSettingsScreen) -> UIViewController,
    private val makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    private val makeAuthController: (IosAuthScreen) -> UIViewController,
    private val makeCheckInCreatePickerController: (IosCheckInCreatePicker) -> UIViewController,
    private val makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    private val makeOwnProfileController: (IosOwnProfile) -> UIViewController,
    private val makePublicProfileController: (IosPublicProfile) -> UIViewController,
    private val makeNotificationInboxController: (IosNotificationInbox, String) -> UIViewController,
) {
    @Composable
    fun Content(onReady: () -> Unit) {
        CompositionLocalProvider(
            LocalIosDiscoveryFilterController provides makeDiscoveryFilterController,
            LocalIosShopMenuGalleryController provides makeShopMenuGalleryController,
            LocalIosShopCheckInListController provides makeShopCheckInListController,
            LocalIosCoffeeDetailController provides makeCoffeeDetailController,
            LocalIosShelfArchiveController provides makeShelfArchiveController,
            LocalIosThemeSettingsController provides makeThemeSettingsController,
            LocalIosShopReportController provides makeShopReportController,
            LocalIosContributionsController provides makeContributionsController,
            LocalIosContentReportController provides makeContentReportController,
            LocalIosCollectionsLibraryController provides makeCollectionsLibraryController,
            LocalIosSavedPublicCollectionController provides makeSavedPublicCollectionController,
            LocalIosSettingsController provides makeSettingsController,
            LocalIosBlockedAuthorsController provides makeBlockedAuthorsController,
            LocalIosAuthController provides makeAuthController,
            LocalCheckInCreatePicker provides iosCheckInCreatePickerContent(makeCheckInCreatePickerController),
            LocalIosGuestProfileController provides makeGuestProfileController,
            LocalIosOwnProfileController provides makeOwnProfileController,
            LocalIosPublicProfileController provides makePublicProfileController,
            LocalIosNotificationInboxController provides makeNotificationInboxController,
        ) {
            App(onReady = onReady)
        }
    }
}
