package com.coffeepeek.admin

import com.coffeepeek.admin.ui.IosNativeRoot
import com.coffeepeek.admin.ui.IosNativeRootControllers
import org.koin.mp.KoinPlatform
import com.coffeepeek.admin.di.coffee.IosCoffeeDetail
import com.coffeepeek.admin.di.discovery.IosDiscoveryFilters
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.admin.di.IosNativeControllerFactories
import com.coffeepeek.admin.di.notifications.IosNotificationInbox
import com.coffeepeek.admin.di.auth.IosAuthScreen
import com.coffeepeek.admin.di.homecheckin.IosCheckInCreatePicker
import com.coffeepeek.admin.di.profile.IosGuestProfile
import com.coffeepeek.admin.di.profile.IosOwnProfile
import com.coffeepeek.admin.di.contributions.IosContributionsRoute
import com.coffeepeek.admin.di.profile.IosPublicProfile
import com.coffeepeek.admin.di.contentreport.IosContentReport
import com.coffeepeek.admin.di.shopreport.IosShopReport
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.admin.di.collections.IosCollectionsWorkspace
import com.coffeepeek.admin.di.settings.IosSettingsScreen
import com.coffeepeek.admin.di.authorblocking.IosBlockedAuthors
import com.coffeepeek.admin.di.shelf.IosShelfArchive
import com.coffeepeek.admin.di.settings.IosThemeSettings
import com.coffeepeek.admin.di.shop.IosShopCheckInList
import com.coffeepeek.admin.di.shop.IosShopMenuGallery
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.appLinkScreen
import com.coffeepeek.core.designsystem.component.configureCoffeePeekNativeGlass
import platform.UIKit.UIViewController
import platform.UIKit.UIVisualEffectView
import platform.UIKit.UIColor

private var koinInitialized = false

fun configureNativeShopDetails(photo: (com.coffeepeek.admin.di.media.IosPhotoViewer, String, () -> Unit) -> UIViewController,
    message: (com.coffeepeek.admin.di.shop.IosShopDetailNotice) -> UIViewController,
    error: (com.coffeepeek.admin.di.shop.IosShopDetailSessionError) -> UIViewController) =
    com.coffeepeek.admin.di.shop.configureNativeShopDetailOverlays(photo, message, error)

fun configureNativeCheckInDetails(factory: (com.coffeepeek.admin.di.community.IosCheckInDetail) -> UIViewController) =
    com.coffeepeek.admin.di.community.configureNativeCheckInDetails(factory)

fun configureNativeDiscovery(factory: (com.coffeepeek.admin.di.discovery.IosDiscovery) -> UIViewController) =
    com.coffeepeek.admin.di.discovery.configureNativeDiscovery(factory)

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

fun configureNativeShelf(factory: (com.coffeepeek.admin.di.shelf.IosShelfScreen) -> UIViewController) =
    com.coffeepeek.admin.di.shelf.configureNativeShelf(factory)

fun configureNativeGlass(makeView: () -> UIVisualEffectView) {
    configureCoffeePeekNativeGlass(makeView)
}

fun configureNativeLoader(factory: (UIColor, Double) -> UIViewController) {
    com.coffeepeek.core.designsystem.component.configureCoffeePeekNativeLoader(factory)
}

fun handleAppLink(link: String): Boolean {
    return com.coffeepeek.admin.di.attribution.AppAttribution.open(link)
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
    makeCollectionsWorkspaceController: (IosCollectionsWorkspace, String) -> UIViewController,
    makeSettingsController: (IosSettingsScreen) -> UIViewController,
    makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    makeAuthController: (IosAuthScreen) -> UIViewController,
    makeCheckInCreatePickerController: (IosCheckInCreatePicker) -> UIViewController,
    makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    makeOwnProfileController: (IosOwnProfile) -> UIViewController,
    makePublicProfileController: (IosPublicProfile) -> UIViewController,
    makeNotificationInboxController: (IosNotificationInbox, String) -> UIViewController,
): UIViewController = IosNativeControllerFactories(
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
    makeCollectionsWorkspaceController,
    makeSettingsController,
    makeBlockedAuthorsController,
    makeAuthController,
    makeCheckInCreatePickerController,
    makeGuestProfileController,
    makeOwnProfileController,
    makePublicProfileController,
    makeNotificationInboxController,
).createController(onReady)

private fun IosNativeControllerFactories.createController(onReady: () -> Unit): UIViewController {
    if (!koinInitialized) {
        initPlatformKoin()
        koinInitialized = true
    }
    return IosNativeRootControllers.controller(IosNativeRoot(KoinPlatform.getKoin(), this, onReady))
}
