package com.coffeepeek.admin.di

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.utils.CustomUrlFetcher
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.ui.NavigatorViewModel
import com.coffeepeek.admin.ui.screen.shop.LegacyShopReportScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.LegacyShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.auth.AuthViewModel
import com.coffeepeek.admin.ui.screen.auth.registr.RegisterViewModel
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.addshop.AddShopViewModel
import com.coffeepeek.admin.ui.screen.editprofile.EditProfileViewModel
import com.coffeepeek.admin.ui.screen.map.MapViewModel
import com.coffeepeek.admin.ui.screen.profile.ProfileViewModel
import com.coffeepeek.admin.ui.screen.deleteaccount.DeleteAccountPendingViewModel
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.favorites.FavoritesViewModel
import com.coffeepeek.admin.ui.screen.review.CreateReviewViewModel
import com.coffeepeek.admin.ui.screen.review.ReviewReportViewModel
import com.coffeepeek.admin.ui.screen.review.EditReviewViewModel
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
        appModule(database.settingRepository, shopReportRendererFactory, shopGalleryRendererFactory),
        imageModule(),
        updateInstallerModule(),
    )
    startKoin { modules(commonModules + platformModules) }
}

private fun appModule(
    settingRepository: com.coffeepeek.room.repository.SettingRepository,
    shopReportRendererFactory: (CoffeePeekClient) -> ShopReportScreenRenderer,
    shopGalleryRendererFactory: (CoffeePeekClient) -> ShopMenuGalleryScreenRenderer,
) = module {
    single<ShopReportScreenRenderer> { shopReportRendererFactory(get()) }
    single<ShopMenuGalleryScreenRenderer> { shopGalleryRendererFactory(get()) }
    single<com.coffeepeek.admin.feature.appupdate.domain.AppUpdateRepository> {
        com.coffeepeek.admin.feature.appupdate.data.AppUpdateRepositoryImpl(get<CoffeePeekClient>().plainClient, settingRepository)
    }
    single { com.coffeepeek.admin.feature.appupdate.ui.AppUpdateState(get()) }
    single<CustomUrlFetcher> { createImageUrlFetcher(get<CoffeePeekClient>().client) }
    single { CheckInDraftStore() }
    single { CityPreference(settingRepository) }
    single { ReviewDraftStore(settingRepository) }
    factory { AuthViewModel(get()) }
    factory { RegisterViewModel(get()) }
    factory { NavigatorViewModel(get()) }
    factory { FeedViewModel(get(), get(), get(), get(), getOrNull<ObserveFavoriteIdsUseCase>()) }
    factory { com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel(get(), get()) }
    factory { MapViewModel(get(), get()) }
    factory { (shopId: String) -> ShopDetailViewModel(shopId, get(), get(), get(), get(), get(), get(),
        userRepository = get(), observeFavoriteIds = getOrNull<ObserveFavoriteIdsUseCase>()) }
    factory { (shopId: String) -> ShopMenuGalleryViewModel(shopId, get()) }
    factory { (shopId: String) -> ShopReportViewModel(shopId, get()) }
    factory { (shopId: String) -> SuggestShopChangeViewModel(shopId, get()) }
    factory { (shopId: String, section: ShopChangeSection, requestId: String) ->
        ShopChangeEditorViewModel(shopId, section, requestId, get(), get())
    }
    factory { (requestId: String) -> ShopChangeRequestDetailViewModel(requestId, get()) }
    single { ProfileViewModel(get(), get(), get(), get(), get(), get()) }
    factory { DeleteAccountPendingViewModel(get(), get()) }
    factory { AddShopViewModel(get()) }
    factory { EditProfileViewModel(get()) }
    factory { FavoritesViewModel(get()) }
    factory { (kind: ContributionKind) -> MyContributionsViewModel(kind, get(), get(), get(), get(), get(), get()) }
    factory { VisitedPlacesViewModel(get(), get()) }
    factory { AddRoasterViewModel(get(), get()) }
    factory { (roasterId: String) -> RoasterDetailViewModel(roasterId, get(), get()) }
    factory { (shopId: String) -> CreateReviewViewModel(shopId, get(), get(), get()) }
    factory { (reviewId: String) -> ReviewReportViewModel(reviewId, get(), get()) }
    factory { (reviewId: String) -> EditReviewViewModel(reviewId, get(), get(), get(), get(), get()) }
}
