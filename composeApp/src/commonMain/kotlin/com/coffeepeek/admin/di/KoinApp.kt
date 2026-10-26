package com.coffeepeek.admin.di

import com.coffeepeek.core.media.createImageUrlFetcher

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.coffee.data.CoffeeRepositoryImpl
import com.coffeepeek.admin.feature.coffee.data.CoffeePickerAdapter
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.data.BrewMethodCatalogAdapter
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import com.coffeepeek.feature.profile.data.createPublicProfileData
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.legacy.homecheckin.data.HomeCheckInRepositoryImpl
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.admin.feature.favorites.data.ServerRoasterFavorites
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.ui.NavigatorViewModel
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.admin.ui.screen.coffee.CoffeeScreenRenderer
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.admin.feature.community.ui.CommunityScreenRenderer
import com.coffeepeek.admin.feature.community.ui.CheckInDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.contributions.ContributionsScreenRenderer
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.OwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.di.roaster.roasterModule
import com.coffeepeek.admin.di.roaster.roasterScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInListScreenRenderer
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.addshop.AddShopViewModel
import com.coffeepeek.admin.ui.screen.map.MapViewModel
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.favorites.FavoritesViewModel
import com.coffeepeek.admin.ui.screen.review.ReviewReportViewModel
import com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.MyContributionsViewModel
import com.coffeepeek.admin.ui.screen.roaster.AddRoasterViewModel
import com.coffeepeek.admin.ui.screen.roaster.RoasterDetailViewModel
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryViewModel
import com.coffeepeek.admin.ui.screen.shop.ShopReportViewModel
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeEditorViewModel
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeRequestDetailViewModel
import com.coffeepeek.admin.ui.screen.shopchange.SuggestShopChangeViewModel
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.admin.di.imageModule
import com.coffeepeek.data.di.dataModule
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun initKoin(
    registerLegacyFavorites: Boolean = true,
    platformModules: List<Module> = emptyList(),
    shopReportRendererFactory: (CoffeePeekClient) -> ShopReportScreenRenderer,
    shopGalleryRendererFactory: (CoffeePeekClient) -> ShopMenuGalleryScreenRenderer,
    shopCheckInRendererFactory: Scope.() -> ShopCheckInFormRenderer,
    shopDetailRendererFactory: Scope.() -> ShopDetailScreenRenderer,
    shopCheckInListRendererFactory: Scope.() -> ShopCheckInListScreenRenderer,
    checkInRenderers: CheckInRendererFactories,
    contentReportRendererFactory: Scope.() -> ContentReportScreenRenderer,
    coffeeRendererFactory: Scope.() -> CoffeeScreenRenderer,
    roasterRendererFactory: Scope.() -> RoasterScreenRenderer = { roasterScreenRenderer(get(), get(), get()) },
    publicProfileRendererFactory: Scope.() -> PublicProfileScreenRenderer,
    ownProfileRendererFactory: Scope.() -> OwnProfileScreenRenderer,
    accountRendererFactory: Scope.() -> AccountScreenRenderer,
    settingsRendererFactory: Scope.() -> SettingsScreenRenderer,
    appUpdateRendererFactory: Scope.() -> AppUpdateRenderer,
    authRendererFactory: Scope.() -> AuthScreenRenderer,
    communityRendererFactory: Scope.() -> CommunityScreenRenderer,
    checkInDetailRendererFactory: Scope.() -> CheckInDetailScreenRenderer,
    contributionsRendererFactory: Scope.() -> ContributionsScreenRenderer,
    discoveryRendererFactory: Scope.() -> DiscoveryScreenRenderer,
    notificationsRendererFactory: Scope.() -> NotificationsRenderer,
    analyticsEnabled: Boolean = true,
) {
    check(AppConfig.baseUrl.isNotBlank()) {
        "API_BASE_URL is not configured. Copy local.properties.example to local.properties."
    }
    val database = Locator.database
    ThemeManager.initialize(database.settingRepository)

    val commonModules = listOf(
        dataModule(
            baseUrl = Constants.BASE_URL,
            cacheFolderPath = Locator.cacheFolderPath,
            appCacheRootPath = Locator.appCacheRootPath,
            database = database,
            platformContext = Locator.platformContext,
            debug = AppConfig.isDebug,
            registerLegacyFavorites = registerLegacyFavorites,
        ),
        appModule(database.settingRepository, shopReportRendererFactory, shopGalleryRendererFactory,
            shopCheckInRendererFactory, shopDetailRendererFactory, shopCheckInListRendererFactory,
            checkInRenderers, contentReportRendererFactory, coffeeRendererFactory, roasterRendererFactory,
            publicProfileRendererFactory, ownProfileRendererFactory, accountRendererFactory, settingsRendererFactory, appUpdateRendererFactory,
            authRendererFactory, communityRendererFactory, checkInDetailRendererFactory, contributionsRendererFactory, discoveryRendererFactory, notificationsRendererFactory),
        imageModule(),
        roasterModule(),
        updateInstallerModule(),
    )
    val application = startKoin { modules(commonModules + platformModules) }
    application.koin.get<com.coffeepeek.feature.notifications.api.NotificationFeature>()
    if (analyticsEnabled) com.coffeepeek.admin.analytics.installAnalytics(database.settingRepository, application.koin.get(), application.koin.get())
    com.coffeepeek.core.media.installImageAccess(
        application.koin.get(), application.koin.get<CoffeePeekClient>().client,
        application.koin.get<com.coffeepeek.feature.authorblocking.api.AuthorVisibility>().visibility,
    )
}

private fun appModule(
    settingRepository: com.coffeepeek.room.repository.SettingRepository,
    shopReportRendererFactory: (CoffeePeekClient) -> ShopReportScreenRenderer,
    shopGalleryRendererFactory: (CoffeePeekClient) -> ShopMenuGalleryScreenRenderer,
    shopCheckInRendererFactory: Scope.() -> ShopCheckInFormRenderer,
    shopDetailRendererFactory: Scope.() -> ShopDetailScreenRenderer,
    shopCheckInListRendererFactory: Scope.() -> ShopCheckInListScreenRenderer,
    checkInRenderers: CheckInRendererFactories,
    contentReportRendererFactory: Scope.() -> ContentReportScreenRenderer,
    coffeeRendererFactory: Scope.() -> CoffeeScreenRenderer,
    roasterRendererFactory: Scope.() -> RoasterScreenRenderer,
    publicProfileRendererFactory: Scope.() -> PublicProfileScreenRenderer,
    ownProfileRendererFactory: Scope.() -> OwnProfileScreenRenderer,
    accountRendererFactory: Scope.() -> AccountScreenRenderer,
    settingsRendererFactory: Scope.() -> SettingsScreenRenderer,
    appUpdateRendererFactory: Scope.() -> AppUpdateRenderer,
    authRendererFactory: Scope.() -> AuthScreenRenderer,
    communityRendererFactory: Scope.() -> CommunityScreenRenderer,
    checkInDetailRendererFactory: Scope.() -> CheckInDetailScreenRenderer,
    contributionsRendererFactory: Scope.() -> ContributionsScreenRenderer,
    discoveryRendererFactory: Scope.() -> DiscoveryScreenRenderer,
    notificationsRendererFactory: Scope.() -> NotificationsRenderer,
) = module {
    single<ContentReportScreenRenderer> { contentReportRendererFactory() }
    single<CoffeeScreenRenderer> { coffeeRendererFactory() }
    single<RoasterScreenRenderer> { roasterRendererFactory() }
    single<PublicProfileScreenRenderer> { publicProfileRendererFactory() }
    single<OwnProfileScreenRenderer> { ownProfileRendererFactory() }
    single<AccountScreenRenderer> { accountRendererFactory() }
    single<SettingsScreenRenderer> { settingsRendererFactory() }
    single<AppUpdateRenderer> { appUpdateRendererFactory() }
    single<NotificationsRenderer> { notificationsRendererFactory() }
    single<AuthScreenRenderer> { authRendererFactory() }
    single<CommunityScreenRenderer> { communityRendererFactory() }
    single<CheckInDetailScreenRenderer> { checkInDetailRendererFactory() }
    single<ContributionsScreenRenderer> { contributionsRendererFactory() }
    single<DiscoveryScreenRenderer> { discoveryRendererFactory() }
    single<com.coffeepeek.feature.community.domain.repository.CommunityRepository> {
        com.coffeepeek.feature.community.data.repository.createCommunityRepository(get<CoffeePeekClient>().client, Constants.BASE_URL)
    }
    single<com.coffeepeek.feature.community.api.CommunityAccount> { com.coffeepeek.admin.di.community.SessionCommunityAccount(get(), get()) }
    single<com.coffeepeek.feature.community.api.ConsumedDrinks> { com.coffeepeek.admin.di.community.ShopCommunityDrinks(get()) }
    single<com.coffeepeek.feature.community.api.CommunityAnalytics> { com.coffeepeek.admin.di.community.AppCommunityAnalytics() }
    single<com.coffeepeek.feature.auth.api.LocalUserData> { com.coffeepeek.admin.di.auth.UserSessionLocalUserData(get()) }
    single<com.coffeepeek.feature.auth.api.AuthAnalytics> { com.coffeepeek.admin.di.auth.AppAuthAnalytics() }
    single<com.coffeepeek.feature.auth.domain.AuthRepository> {
        val gateway = get<CoffeePeekClient>()
        com.coffeepeek.feature.auth.data.createAuthRepository(gateway.plainClient, gateway.client, get(), get())
    }
    single<com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer> { checkInRenderers.shopPicker(this) }
    single<CheckInCreateFlowRenderer> { checkInRenderers.create(this) }
    single<CheckInHistoryScreenRenderer> { checkInRenderers.history(this) }
    single<ShopDetailScreenRenderer> { shopDetailRendererFactory() }
    single<ShopCheckInListScreenRenderer> { shopCheckInListRendererFactory() }
    single<ShopCheckInFormRenderer> { shopCheckInRendererFactory() }
    single<ShopReportScreenRenderer> { shopReportRendererFactory(get()) }
    single<ShopMenuGalleryScreenRenderer> { shopGalleryRendererFactory(get()) }
    single<com.coffeepeek.feature.notifications.api.NotificationFeature> {
        com.coffeepeek.feature.notifications.impl.createNotificationFeature(
            com.coffeepeek.feature.notifications.data.createNotificationRepository(get<CoffeePeekClient>().client),
            com.coffeepeek.admin.di.notifications.SessionNotificationAccount(get()), com.coffeepeek.admin.notifications.NativePushBridge,
        )
    }
    single<CoffeeRepository> { CoffeeRepositoryImpl(get<CoffeePeekClient>().plainClient) }
    single<CoffeePicker> { CoffeePickerAdapter(get()) }
    single<BrewMethodCatalog> { BrewMethodCatalogAdapter(get()) }
    single<HomeCheckInRepository> {
        val gateway = get<CoffeePeekClient>()
        HomeCheckInRepositoryImpl(gateway.client, gateway.uploadClient, Constants.BASE_URL)
    }
    factory { params ->
        HomeCheckInViewModel(get(), get(), get(), onCheckInCreated = get<NotificationFeature>()::onCheckInCreated,
            calendarWeek = params.getOrNull<com.coffeepeek.admin.ui.screen.checkins.CalendarWeek>())
    }
    single { com.coffeepeek.feature.authorblocking.impl.createAuthorBlockingFeature(get<CoffeePeekClient>().client, com.coffeepeek.admin.di.authorblocking.SessionAuthorBlockingAccount(get())) }
    single<com.coffeepeek.feature.authorblocking.api.AuthorVisibility> { get<com.coffeepeek.feature.authorblocking.impl.AuthorBlockingFeature>().visibility }
    single<com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter> { get<com.coffeepeek.feature.authorblocking.impl.AuthorBlockingFeature>().presenter }
    single<com.coffeepeek.feature.authorblocking.api.AuthorBlockingEntry> { com.coffeepeek.feature.authorblocking.impl.createAuthorBlockingEntry(get()) }
    single<com.coffeepeek.feature.authorblocking.api.AuthorBlockActions> { get<com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter>() }
    single<com.coffeepeek.feature.profile.domain.PublicProfileData> {
        createPublicProfileData(get<CoffeePeekClient>().client,
            com.coffeepeek.feature.profile.data.repository.createPublicProfileFavoriteDetails(get(), get()))
    }
    single<com.coffeepeek.feature.profile.data.repository.ProfileAccountData> {
        val client = get<CoffeePeekClient>()
        com.coffeepeek.feature.profile.data.repository.createProfileAccountData(client.client, client.uploadClient, get(), get())
    }
    single<com.coffeepeek.feature.profile.api.ProfileAccountPort> { get<com.coffeepeek.feature.profile.data.repository.ProfileAccountData>().accounts }
    single<com.coffeepeek.feature.profile.api.ProfileEditPort> { get<com.coffeepeek.feature.profile.data.repository.ProfileAccountData>().edit }
    single<com.coffeepeek.feature.profile.api.AccountDeletionPort> { get<com.coffeepeek.feature.profile.data.repository.ProfileAccountData>().deletion }
    single<com.coffeepeek.feature.shop.api.PublicShopDetails> {
        com.coffeepeek.feature.shop.data.repository.createPublicShopDetails(get<CoffeePeekClient>().client)
    }
    single<com.coffeepeek.feature.roaster.api.PublicRoasterDetails> {
        com.coffeepeek.feature.roaster.data.repository.createPublicRoasterDetails(get())
    }
    single<com.coffeepeek.feature.community.api.PublicAuthorPosts> {
        com.coffeepeek.feature.community.data.repository.createPublicAuthorPosts(get())
    }
    single<com.coffeepeek.feature.profile.api.PublicCheckInsPort> {
        com.coffeepeek.feature.profile.data.repository.createPublicProfileCheckIns(get())
    }
    single<RoasterFavorites> { ServerRoasterFavorites(get(), get(), get()) }
    single<com.coffeepeek.feature.appupdate.domain.AppUpdateRepository> {
        com.coffeepeek.feature.appupdate.data.AppUpdateRepositoryImpl(
            get<CoffeePeekClient>().plainClient,
            com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment,
            com.coffeepeek.admin.di.appupdate.SettingsDismissedUpdateStore(settingRepository),
        )
    }
    single { com.coffeepeek.feature.appupdate.impl.AppUpdateController(get(), com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment) }
    single<ImageUrlFetcher> {
        val gateway = get<CoffeePeekClient>()
        createImageUrlFetcher(gateway.client, gateway.imageClient)
    }
    single { CheckInDraftStore() }
    single { CityPreference(settingRepository) }
    single { ReviewDraftStore(settingRepository) }
    factory { NavigatorViewModel(get()) }
    factory { FeedViewModel(get(), get(), get(), get(), getOrNull<ObserveFavoriteIdsUseCase>()) }
    factory { com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel(get(), get(), get()) }
    factory { MapViewModel(get(), get()) }
    factory { (shopId: String) -> ShopMenuGalleryViewModel(shopId, get()) }
    factory { (shopId: String) -> ShopReportViewModel(shopId, get()) }
    factory { (shopId: String) -> SuggestShopChangeViewModel(shopId, get()) }
    factory { (shopId: String, section: ShopChangeSection, requestId: String) ->
        ShopChangeEditorViewModel(shopId, section, requestId, get(), get())
    }
    factory { (requestId: String) -> ShopChangeRequestDetailViewModel(requestId, get()) }
    factory { AddShopViewModel(get()) }
    single { FavoritesViewModel(get(), get(), get(), get(), sessions = get()) }
    factory { (kind: ContributionKind) -> MyContributionsViewModel(kind, get(), get(), get()) }
    factory { params -> VisitedPlacesViewModel(get(), get(), params.getOrNull()) }
    factory { AddRoasterViewModel(get(), get()) }
    factory { (roasterId: String) -> RoasterDetailViewModel(roasterId, get(), get(), get(), get()) }
    factory { (reviewId: String, isPreview: Boolean, isCheckIn: Boolean) -> ReviewReportViewModel(reviewId, get(), get(), isPreview, get(), isCheckIn) }
}
