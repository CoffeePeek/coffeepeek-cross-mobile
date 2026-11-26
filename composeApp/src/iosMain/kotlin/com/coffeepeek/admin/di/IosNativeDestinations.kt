package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.auth.*
import com.coffeepeek.admin.di.authorblocking.iosBlockedAuthorsContent
import com.coffeepeek.admin.di.checkinhistory.IosNativeHomeCheckInFactory
import com.coffeepeek.admin.di.coffee.iosCoffeeDetailContent
import com.coffeepeek.admin.di.collections.*
import com.coffeepeek.admin.di.community.*
import com.coffeepeek.admin.di.contributions.*
import com.coffeepeek.admin.di.contentreport.iosContentReportContent
import com.coffeepeek.admin.di.discovery.*
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.ownership.IosNativeOwnershipFactory
import com.coffeepeek.admin.di.profile.*
import com.coffeepeek.admin.di.roaster.*
import com.coffeepeek.admin.di.settings.*
import com.coffeepeek.admin.di.shelf.*
import com.coffeepeek.admin.di.shop.*
import com.coffeepeek.admin.di.shopreport.iosShopReportContent
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.RootNavigationOrigin
import com.coffeepeek.admin.ui.component.NativeReviewDateFormatter
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.Koin
import platform.UIKit.UIViewController

/** Application composition only. Every returned screen owns its feature lifetime. */
internal class IosNativeDestinations(private val dependencies: Koin, private val ui: IosNativeControllerFactories,
    parent: CoroutineScope, private val loggedIn: () -> Boolean, private val resetLogin: () -> Unit,
    private val mapExpanded: (Boolean) -> Unit, private val reselections: Flow<Unit>,
    private val registerRefresh: (() -> Unit) -> (() -> Unit),
    private val navigationOrigin: RootNavigationOrigin,
) {
    private val language = dependencies.get<LanguagePort>()
    private val media = IosCatalogueMedia(dependencies.get(), Constants.BASE_URL)
    private val dates = NativeReviewDateFormatter(parent, language.observe().map { it.languageTag })
    private val workspace = iosCollectionsWorkspaceContent(ui.makeCollectionsWorkspaceController, ::languageTag,
        dependencies.get(), dependencies.get(), dependencies.get()) { Navigator.returnToProfile(navigationOrigin) }
    private val shelf = iosShelfFactory(dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(), language, media)
    private val auth = iosAuthContent(ui.makeAuthController, dependencies.get(),
        IosAuthDestinations(dependencies.get(), dependencies.get(), language))
    private val settings = IosSettingsDependencies(dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(),
        language, dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(), parent)
    private val community = community()
    private val roaster = IosNativeRoasterFactory(dependencies.get(), dependencies.get(), dependencies.get(), language,
        dependencies.get(), media, dependencies.get())
    private val ownership = IosNativeOwnershipFactory(dependencies.get(), language, media)
    private val shop = shop()
    private val shopCheckIn = IosNativeShopCheckInFactory(dependencies.get(), dependencies.get(),
        dependencies.get<ShopCheckInCreateEntry>() as SessionShopCheckInEntry, language)
    private val home = IosNativeHomeCheckInFactory(dependencies.get(), dependencies.get(), language, dependencies.get())
    private val contributions = contributions()
    private val own = ownProfile()
    private val publicProfileController = publicProfile()
    private val guest = iosGuestProfileContent(ui.makeGuestProfileController, language)
    private val edit = IosNativeEditProfileFactory(dependencies.get(), dependencies.get(), language, media)

    fun controller(screen: Navigator.Screen): UIViewController {
        return (auth(screen, resetLogin) ?: settings.controller(screen, ui.makeSettingsController)
            ?: profiles(screen) ?: catalog(screen) ?: checkIns(screen) ?: collections(screen) ?: contributions(screen)
            ?: reports(screen) ?: other(screen) ?: error("No native destination for ${screen::class.simpleName}"))()
    }
    private fun languageTag() = language.current().languageTag
    private fun profiles(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        Navigator.Screen.ProfileTab -> if (loggedIn()) own else guest
        Navigator.Screen.EditProfile -> edit::controller
        is Navigator.Screen.UserProfile -> { { publicProfileController(screen.slug, screen.nickname, screen.avatarUrl, false) } }
        is Navigator.Screen.UserCheckIns -> { { publicProfileController(screen.slug, screen.nickname, screen.avatarUrl, true) } }
        else -> null
    }
    private fun catalog(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        Navigator.Screen.FeedTab -> { { discovery() } }
        is Navigator.Screen.ShopDetail -> { { shop.controller(screen.shopId, screen.openCheckIn) } }
        is Navigator.Screen.RoasterDetail -> { { roasterController(roaster.detail(screen.roasterId)) } }
        is Navigator.Screen.CoffeeDetail -> { { iosCoffeeDetailContent(ui.makeCoffeeDetailController, ::languageTag, dependencies.get(), media)(screen.slug) } }
        Navigator.Screen.BusinessTab -> { { ownership.controller(true, null, null, null) } }
        is Navigator.Screen.MyBusiness -> { { ownership.controller(false, screen.kind, screen.slug, screen.businessId) } }
        else -> null
    }
    private fun checkIns(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        Navigator.Screen.CommunityTab -> { { community.feed(registerRefresh) } }
        is Navigator.Screen.CheckInDetail -> { { community.post(screen.id) } }
        is Navigator.Screen.HomeCheckInDetail -> { { home.controller(screen.id, false) } }
        is Navigator.Screen.EditHomeCheckIn -> { { home.controller(screen.id, true) } }
        is Navigator.Screen.ShopCheckInEditor -> { { shopCheckIn.controller(screen.shopId) } }
        Navigator.Screen.CreateCheckIn -> { { IosNativeCheckInShopPickerFactory(
            dependencies.get<CheckInShopPickerRenderer>() as NativeCheckInShopPickerSessionFactory,
            dependencies.get(), language, media).controller() } }
        is Navigator.Screen.ShopReviews -> { { reviewList(screen.shopId) } }
        else -> null
    }
    private fun collections(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        Navigator.Screen.CollectionsComposer, Navigator.Screen.CollectionsLibrary ->
            iosCollectionsLibraryContent(ui.makeCollectionsLibraryController, ::languageTag, dependencies.get(), dependencies.get(), dependencies.get())
        is Navigator.Screen.PublicCollection -> { { workspace("Snapshot", screen.id, null) } }
        is Navigator.Screen.SavedPublicCollection -> { { workspace("Public", screen.id, null) } }
        is Navigator.Screen.CollectionEditor -> { { workspace("Editor", screen.id, null) } }
        else -> null
    }
    private fun reports(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        is Navigator.Screen.ReportShop -> iosShopReportContent(ui.makeShopReportController, ::languageTag, dependencies.get(), dependencies.get())(screen.shopId, screen.shopTitle)
        is Navigator.Screen.ReportReview -> iosContentReportContent(ui.makeContentReportController, ::languageTag, dependencies.get(), dependencies.get())(screen.reviewId, false, screen.isPreview)
        is Navigator.Screen.ReportCheckIn -> iosContentReportContent(ui.makeContentReportController, ::languageTag, dependencies.get(), dependencies.get())(screen.checkInId, true, false)
        else -> null
    }
    private fun other(screen: Navigator.Screen): (() -> UIViewController)? = when (screen) {
        Navigator.Screen.ThemeSettings -> iosThemeSettingsContent(ui.makeThemeSettingsController, ::languageTag)
        Navigator.Screen.BlockedAuthors -> iosBlockedAuthorsContent(ui.makeBlockedAuthorsController, ::languageTag, dependencies.get())
        Navigator.Screen.ShelfArchive -> iosShelfArchiveContent(ui.makeShelfArchiveController, ::languageTag, dependencies.get(), dependencies.get(), media)
        Navigator.Screen.ShelfAddBag -> { { nativeShelfController(shelf("add", null, true) { true }) } }
        is Navigator.Screen.ShelfBag -> { { nativeShelfController(shelf("bag", screen.bagId, true) { true }) } }
        is Navigator.Screen.ShopMenuGallery -> { { gallery(screen.shopId) } }
        else -> null
    }
    private fun discovery(): UIViewController = IosDiscoveryRouteControllers.controller(IosDiscoveryRoute(language, { expanded ->
        IosNativeDiscoveryOwner(dependencies.get(), dependencies.get(), language, media, dependencies.get(),
            dependencies.get(), dependencies.get(), reselections, expanded).source
    }, mapExpanded))
    private fun community() = IosNativeCommunityFactory(dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(),
        dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(), language, dependencies.get(), dependencies.get(), dependencies.get())
    private fun shop() = IosNativeShopDetailFactory(dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(),
        dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(),
        dependencies.get(), dependencies.get<ShopCheckInCreateEntry>() as SessionShopCheckInEntry, language, dependencies.get(), dependencies.get())
    private fun contributions() = iosContributionsContent(ui.makeContributionsController, ::languageTag, dependencies.get(), dependencies.get(),
        iosContributionFormsContent(ui.makeContributionsController, ::languageTag, dependencies.get(), dependencies.get(), dependencies.get(), media))
    private fun ownProfile() = iosOwnProfileContent(
        factory = ui.makeOwnProfileController,
        shelf = shelf,
        accounts = dependencies.get(),
        notifications = dependencies.get(),
        auth = dependencies.get(),
        drafts = dependencies.get(),
        repository = dependencies.get(),
        checkIns = dependencies.get(),
        likes = dependencies.get(),
        visibility = dependencies.get(),
        snapshots = dependencies.get(),
        sessions = dependencies.get(),
        shops = dependencies.get(),
        homes = dependencies.get(),
        calendar = dependencies.get(),
        language = language,
        media = media,
        collections = workspace,
    )
    private fun publicProfile() = iosPublicProfileContent(
        factory = ui.makePublicProfileController,
        shelf = shelf,
        accounts = dependencies.get(),
        data = dependencies.get(),
        checkIns = dependencies.get(),
        likes = dependencies.get(),
        visibility = dependencies.get(),
        blocks = dependencies.get(),
        snapshots = dependencies.get(),
        language = language,
        media = media,
        collections = workspace,
    )
    private fun reviewList(id: String) = iosShopCheckInListContent(ui.makeShopCheckInListController, ::languageTag, dependencies.get(),
        dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(), dependencies.get(), media, dates::format)(id)
    private fun gallery(id: String) = iosShopMenuGalleryContent(ui.makeShopMenuGalleryController, ::languageTag,
        createShopMenuGalleryRepository(dependencies.get<ApiClients>().client), media)(id)
}
