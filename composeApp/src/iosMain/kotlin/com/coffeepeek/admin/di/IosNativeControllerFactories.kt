package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.auth.IosAuthScreen
import com.coffeepeek.admin.di.authorblocking.IosBlockedAuthors
import com.coffeepeek.admin.di.coffee.IosCoffeeDetail
import com.coffeepeek.admin.di.collections.IosCollectionsLibrary
import com.coffeepeek.admin.di.collections.IosCollectionsWorkspace
import com.coffeepeek.admin.di.contentreport.IosContentReport
import com.coffeepeek.admin.di.contributions.IosContributionsRoute
import com.coffeepeek.admin.di.discovery.IosDiscoveryFilters
import com.coffeepeek.admin.di.homecheckin.IosCheckInCreatePicker
import com.coffeepeek.admin.di.notifications.IosNotificationInbox
import com.coffeepeek.admin.di.profile.IosGuestProfile
import com.coffeepeek.admin.di.profile.IosOwnProfile
import com.coffeepeek.admin.di.profile.IosPublicProfile
import com.coffeepeek.admin.di.settings.IosSettingsScreen
import com.coffeepeek.admin.di.settings.IosThemeSettings
import com.coffeepeek.admin.di.shelf.IosShelfArchive
import com.coffeepeek.admin.di.shop.IosShopCheckInList
import com.coffeepeek.admin.di.shop.IosShopMenuGallery
import com.coffeepeek.admin.di.shopreport.IosShopReport
import platform.UIKit.UIViewController

/** Constructor-supplied Swift UI boundary, independent of any composition. */
internal class IosNativeControllerFactories(
    val makeDiscoveryFilterController: (IosDiscoveryFilters) -> UIViewController,
    val makeShopMenuGalleryController: (IosShopMenuGallery, String) -> UIViewController,
    val makeShopCheckInListController: (IosShopCheckInList, String) -> UIViewController,
    val makeCoffeeDetailController: (IosCoffeeDetail, String) -> UIViewController,
    val makeShelfArchiveController: (IosShelfArchive, String) -> UIViewController,
    val makeThemeSettingsController: (IosThemeSettings, String, () -> Unit) -> UIViewController,
    val makeShopReportController: (IosShopReport, String, String, () -> Unit) -> UIViewController,
    val makeContributionsController: (IosContributionsRoute, String) -> UIViewController,
    val makeContentReportController: (IosContentReport, String) -> UIViewController,
    val makeCollectionsLibraryController: (IosCollectionsLibrary, String) -> UIViewController,
    val makeCollectionsWorkspaceController: (IosCollectionsWorkspace, String) -> UIViewController,
    val makeSettingsController: (IosSettingsScreen) -> UIViewController,
    val makeBlockedAuthorsController: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    val makeAuthController: (IosAuthScreen) -> UIViewController,
    val makeCheckInCreatePickerController: (IosCheckInCreatePicker) -> UIViewController,
    val makeGuestProfileController: (IosGuestProfile) -> UIViewController,
    val makeOwnProfileController: (IosOwnProfile) -> UIViewController,
    val makePublicProfileController: (IosPublicProfile) -> UIViewController,
    val makeNotificationInboxController: (IosNotificationInbox, String) -> UIViewController,
)
