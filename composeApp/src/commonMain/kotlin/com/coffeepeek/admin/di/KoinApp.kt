package com.coffeepeek.admin.di

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
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListViewModel
import com.coffeepeek.admin.feature.coffee.ui.CoffeeDetailViewModel
import com.coffeepeek.admin.feature.passwordrecovery.data.PasswordRecoveryRepositoryImpl
import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.admin.feature.passwordrecovery.ui.PasswordRecoveryViewModel
import com.coffeepeek.admin.feature.profile.data.AppPublicProfileRepository
import com.coffeepeek.admin.feature.profile.data.LegacyPublicProfileFavoriteDetails
import com.coffeepeek.feature.profile.data.createPublicProfileData
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.admin.feature.profile.ui.PublicProfileViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.legacy.homecheckin.data.HomeCheckInRepositoryImpl
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.admin.feature.favorites.data.ServerRoasterFavorites
import com.coffeepeek.admin.utils.CustomUrlFetcher
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.ui.NavigatorViewModel
import com.coffeepeek.admin.ui.screen.shop.LegacyShopReportScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.LegacyShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInFormRenderer
import com.coffeepeek.admin.ui.screen.shop.LegacyShopCheckInFormRenderer
import com.coffeepeek.admin.ui.screen.coffee.CoffeeScreenRenderer
import com.coffeepeek.admin.ui.screen.coffee.LegacyCoffeeScreenRenderer
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.admin.feature.appupdate.ui.LegacyAppUpdateRenderer
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.LegacyAccountScreenRenderer
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.admin.ui.screen.auth.LegacyAuthScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.LegacySettingsScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.LegacyOwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.LegacyPublicProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.OwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.di.roaster.roasterModule
import com.coffeepeek.admin.di.roaster.roasterScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.LegacyShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInListScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.LegacyShopCheckInListScreenRenderer
import com.coffeepeek.admin.ui.screen.auth.AuthViewModel
import com.coffeepeek.admin.ui.screen.auth.registr.RegisterViewModel
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.addshop.AddShopViewModel
import com.coffeepeek.admin.ui.screen.editprofile.EditProfileViewModel
import com.coffeepeek.admin.ui.screen.map.MapViewModel
import com.coffeepeek.admin.ui.screen.profile.ProfileViewModel
import com.coffeepeek.admin.ui.screen.deleteaccount.DeleteAccountPendingViewModel
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.favorites.FavoritesViewModel
import com.coffeepeek.admin.ui.screen.review.ReviewReportViewModel
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.MyContributionsViewModel
import com.coffeepeek.admin.ui.screen.roaster.AddRoasterViewModel
import com.coffeepeek.admin.ui.screen.roaster.RoasterDetailViewModel
import com.coffeepeek.admin.ui.screen.shop.ShopDetailViewModel
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
    shopReportRendererFactory: (CoffeePeekClient) -> ShopReportScreenRenderer = {
        LegacyShopReportScreenRenderer
    },
    shopGalleryRendererFactory: (CoffeePeekClient) -> ShopMenuGalleryScreenRenderer = {
        LegacyShopMenuGalleryScreenRenderer
    },
    shopCheckInRendererFactory: Scope.() -> ShopCheckInFormRenderer = { LegacyShopCheckInFormRenderer },
    shopDetailRendererFactory: Scope.() -> ShopDetailScreenRenderer = { LegacyShopDetailScreenRenderer },
    shopCheckInListRendererFactory: Scope.() -> ShopCheckInListScreenRenderer = { LegacyShopCheckInListScreenRenderer },
    checkInHistoryRendererFactory: Scope.() -> CheckInHistoryScreenRenderer = { LegacyCheckInHistoryScreenRenderer },
    checkInCreateFlowRendererFactory: Scope.() -> CheckInCreateFlowRenderer = { LegacyCheckInCreateFlowRenderer },
    coffeeRendererFactory: Scope.() -> CoffeeScreenRenderer = { LegacyCoffeeScreenRenderer },
    roasterRendererFactory: Scope.() -> RoasterScreenRenderer = { roasterScreenRenderer(get(), get(), get()) },
    publicProfileRendererFactory: Scope.() -> PublicProfileScreenRenderer = { LegacyPublicProfileScreenRenderer },
    ownProfileRendererFactory: Scope.() -> OwnProfileScreenRenderer = { LegacyOwnProfileScreenRenderer },
    accountRendererFactory: Scope.() -> AccountScreenRenderer = { LegacyAccountScreenRenderer },
    settingsRendererFactory: Scope.() -> SettingsScreenRenderer = { LegacySettingsScreenRenderer },
    appUpdateRendererFactory: Scope.() -> AppUpdateRenderer = { LegacyAppUpdateRenderer },
    authRendererFactory: Scope.() -> AuthScreenRenderer = { LegacyAuthScreenRenderer },
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
            checkInHistoryRendererFactory, checkInCreateFlowRendererFactory, coffeeRendererFactory, roasterRendererFactory,
            publicProfileRendererFactory, ownProfileRendererFactory, accountRendererFactory, settingsRendererFactory, appUpdateRendererFactory,
            authRendererFactory),
        imageModule(),
        roasterModule(),
        updateInstallerModule(),
    )
    val application = startKoin { modules(commonModules + platformModules) }
    application.koin.get<com.coffeepeek.feature.notifications.api.NotificationFeature>()
    if (analyticsEnabled) com.coffeepeek.admin.analytics.installAnalytics(database.settingRepository, application.koin.get(), application.koin.get())
    com.coffeepeek.admin.utils.image.ProtectedPhotoSession.install(
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
    checkInHistoryRendererFactory: Scope.() -> CheckInHistoryScreenRenderer,
    checkInCreateFlowRendererFactory: Scope.() -> CheckInCreateFlowRenderer,
    coffeeRendererFactory: Scope.() -> CoffeeScreenRenderer,
    roasterRendererFactory: Scope.() -> RoasterScreenRenderer,
    publicProfileRendererFactory: Scope.() -> PublicProfileScreenRenderer,
    ownProfileRendererFactory: Scope.() -> OwnProfileScreenRenderer,
    accountRendererFactory: Scope.() -> AccountScreenRenderer,
    settingsRendererFactory: Scope.() -> SettingsScreenRenderer,
    appUpdateRendererFactory: Scope.() -> AppUpdateRenderer,
    authRendererFactory: Scope.() -> AuthScreenRenderer,
) = module {
    single<CoffeeScreenRenderer> { coffeeRendererFactory() }
    single<RoasterScreenRenderer> { roasterRendererFactory() }
    single<PublicProfileScreenRenderer> { publicProfileRendererFactory() }
    single<OwnProfileScreenRenderer> { ownProfileRendererFactory() }
    single<AccountScreenRenderer> { accountRendererFactory() }
    single<SettingsScreenRenderer> { settingsRendererFactory() }
    single<AppUpdateRenderer> { appUpdateRendererFactory() }
    single<AuthScreenRenderer> { authRendererFactory() }
    single<com.coffeepeek.feature.auth.api.LocalUserData> { com.coffeepeek.admin.di.auth.UserSessionLocalUserData(get()) }
    single<com.coffeepeek.feature.auth.api.AuthAnalytics> { com.coffeepeek.admin.di.auth.AppAuthAnalytics() }
    single<com.coffeepeek.feature.auth.domain.AuthRepository> {
        val gateway = get<CoffeePeekClient>()
        com.coffeepeek.feature.auth.data.createAuthRepository(gateway.plainClient, gateway.client, get(), get())
    }
    single<CheckInCreateFlowRenderer> { checkInCreateFlowRendererFactory() }
    single<CheckInHistoryScreenRenderer> { checkInHistoryRendererFactory() }
    single<ShopDetailScreenRenderer> { shopDetailRendererFactory() }
    single<ShopCheckInListScreenRenderer> { shopCheckInListRendererFactory() }
    single<ShopCheckInFormRenderer> { shopCheckInRendererFactory() }
    single<ShopReportScreenRenderer> { shopReportRendererFactory(get()) }
    single<ShopMenuGalleryScreenRenderer> { shopGalleryRendererFactory(get()) }
    single<com.coffeepeek.feature.notifications.api.NotificationFeature> {
        com.coffeepeek.feature.notifications.api.createNotificationFeature(get<CoffeePeekClient>().client, get(), com.coffeepeek.admin.notifications.NativePushBridge)
    }
    single<PasswordRecoveryRepository> { PasswordRecoveryRepositoryImpl(get<CoffeePeekClient>().plainClient) }
    factory { (email: String) -> PasswordRecoveryViewModel(email, get()) }
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
    factory { CoffeeListViewModel(get()) }
    factory { (slug: String) -> CoffeeDetailViewModel(slug, get()) }
    single { com.coffeepeek.feature.authorblocking.impl.createAuthorBlockingFeature(get<CoffeePeekClient>().client, com.coffeepeek.admin.di.authorblocking.SessionAuthorBlockingAccount(get())) }
    single<com.coffeepeek.feature.authorblocking.api.AuthorVisibility> { get<com.coffeepeek.feature.authorblocking.impl.AuthorBlockingFeature>().visibility }
    single<com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter> { get<com.coffeepeek.feature.authorblocking.impl.AuthorBlockingFeature>().presenter }
    single<com.coffeepeek.feature.authorblocking.api.AuthorBlockActions> { get<com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter>() }
    single<PublicProfileRepository> {
        AppPublicProfileRepository(
            createPublicProfileData(get<CoffeePeekClient>().client, LegacyPublicProfileFavoriteDetails(get(), get())),
            get(),
        )
    }
    factory { (slug: String) -> PublicProfileViewModel(slug, get(), get(), users = get(), visibility = get()) }
    single<RoasterFavorites> { ServerRoasterFavorites(get(), get(), get()) }
    single<com.coffeepeek.feature.appupdate.domain.AppUpdateRepository> {
        com.coffeepeek.feature.appupdate.data.AppUpdateRepositoryImpl(
            get<CoffeePeekClient>().plainClient,
            com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment,
            com.coffeepeek.admin.di.appupdate.SettingsDismissedUpdateStore(settingRepository),
        )
    }
    single { com.coffeepeek.feature.appupdate.impl.AppUpdateController(get(), com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment) }
    single<CustomUrlFetcher> {
        val gateway = get<CoffeePeekClient>()
        createImageUrlFetcher(gateway.client, gateway.imageClient)
    }
    single { CheckInDraftStore() }
    single { CityPreference(settingRepository) }
    single { ReviewDraftStore(settingRepository) }
    factory { AuthViewModel(get()) }
    factory { RegisterViewModel(get()) }
    factory { NavigatorViewModel(get()) }
    factory { FeedViewModel(get(), get(), get(), get(), getOrNull<ObserveFavoriteIdsUseCase>()) }
    factory { com.coffeepeek.admin.feature.community.ui.CommunityViewModel(get(), get(), get(), get(), get(), get()) }
    factory { com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel(get(), get(), get()) }
    factory { MapViewModel(get(), get()) }
    factory { (shopId: String) -> ShopDetailViewModel(
        shopId = shopId,
        shopRepository = get(),
        favoriteRepository = get(),
        checkInRepository = get(),
        sessionRepository = get(),
        checkInDraftStore = get(),
        userRepository = get(),
        observeFavoriteIds = getOrNull<ObserveFavoriteIdsUseCase>(),
        roasterFavorites = get(),
        visibility = get(),
        onCheckInCreated = get<NotificationFeature>()::onCheckInCreated,
    ) }
    factory { (shopId: String) -> ShopMenuGalleryViewModel(shopId, get()) }
    factory { (shopId: String) -> ShopReportViewModel(shopId, get()) }
    factory { (shopId: String) -> SuggestShopChangeViewModel(shopId, get()) }
    factory { (shopId: String, section: ShopChangeSection, requestId: String) ->
        ShopChangeEditorViewModel(shopId, section, requestId, get(), get())
    }
    factory { (requestId: String) -> ShopChangeRequestDetailViewModel(requestId, get()) }
    single { ProfileViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { DeleteAccountPendingViewModel(get(), get()) }
    factory { AddShopViewModel(get()) }
    factory { EditProfileViewModel(get()) }
    single { FavoritesViewModel(get(), get(), get(), get(), sessions = get()) }
    factory { (kind: ContributionKind) -> MyContributionsViewModel(kind, get(), get(), get()) }
    factory { params -> VisitedPlacesViewModel(get(), get(), params.getOrNull()) }
    factory { AddRoasterViewModel(get(), get()) }
    factory { (roasterId: String) -> RoasterDetailViewModel(roasterId, get(), get(), get(), get()) }
    factory { (reviewId: String, isPreview: Boolean, isCheckIn: Boolean) -> ReviewReportViewModel(reviewId, get(), get(), isPreview, get(), isCheckIn) }
}
