package com.coffeepeek.admin.di

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.coffee.data.CoffeeRepositoryImpl
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListViewModel
import com.coffeepeek.admin.feature.coffee.ui.CoffeeDetailViewModel
import com.coffeepeek.admin.feature.favorites.data.LocalRoasterFavorites
import com.coffeepeek.admin.utils.CustomUrlFetcher
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.ui.NavigatorViewModel
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
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin() {
    check(AppConfig.baseUrl.isNotBlank()) {
        "API_BASE_URL is not configured. Copy local.properties.example to local.properties."
    }
    val database = Locator.database
    ThemeManager.initialize(database.settingRepository)

    startKoin {
        modules(
            dataModule(
                baseUrl = Constants.BASE_URL,
                cacheFolderPath = Locator.cacheFolderPath,
                appCacheRootPath = Locator.appCacheRootPath,
                database = database,
                platformContext = Locator.platformContext,
                debug = AppConfig.isDebug,
            ),
            appModule(database.settingRepository),
            imageModule(),
            updateInstallerModule(),
        )
    }
}

private fun appModule(settingRepository: com.coffeepeek.room.repository.SettingRepository) = module {
    single<CoffeeRepository> { CoffeeRepositoryImpl(get<CoffeePeekClient>().plainClient) }
    factory { CoffeeListViewModel(get()) }
    factory { (slug: String) -> CoffeeDetailViewModel(slug, get()) }
    single<RoasterFavorites> { LocalRoasterFavorites(settingRepository, get()) }
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
    factory { FeedViewModel(get(), get(), get(), get()) }
    factory { com.coffeepeek.admin.feature.community.ui.CommunityViewModel(get(), get(), get()) }
    factory { com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel(get(), get(), get()) }
    factory { MapViewModel(get(), get()) }
    factory { (shopId: String) -> ShopDetailViewModel(shopId, get(), get(), get(), get(), get(), get(), get(), get()) }
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
    factory { FavoritesViewModel(get(), get(), get()) }
    factory { (kind: ContributionKind) -> MyContributionsViewModel(kind, get(), get(), get()) }
    factory { VisitedPlacesViewModel(get(), get()) }
    factory { AddRoasterViewModel(get(), get()) }
    factory { (roasterId: String) -> RoasterDetailViewModel(roasterId, get(), get(), get(), get()) }
    factory { (reviewId: String, isPreview: Boolean) -> ReviewReportViewModel(reviewId, get(), get(), isPreview) }
}
