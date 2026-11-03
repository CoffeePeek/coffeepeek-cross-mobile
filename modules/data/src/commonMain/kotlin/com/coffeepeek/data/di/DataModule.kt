package com.coffeepeek.data.di

import com.coffeepeek.api.CoffeePeekRepo
import com.coffeepeek.data.repository.CheckInRepositoryImpl
import com.coffeepeek.data.repository.FavoriteRepositoryImpl
import com.coffeepeek.data.repository.PhotoRepositoryImpl
import com.coffeepeek.data.repository.ReviewRepositoryImpl
import com.coffeepeek.data.repository.RoasterRepositoryImpl
import com.coffeepeek.data.repository.ShopChangeRequestRepositoryImpl
import com.coffeepeek.data.repository.ShopIssueReportRepositoryImpl
import com.coffeepeek.data.repository.ShopRepositoryImpl
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopIssueReportRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.room.DatabaseCore
import org.koin.core.module.Module
import org.koin.dsl.module

fun dataModule(
    baseUrl: String,
    database: DatabaseCore,
    registerLegacyFavorites: Boolean = true,
): Module = module {
    single { FileUrlResolver(baseUrl) }
    single { CoffeePeekRepo(get()) }
    single { get<CoffeePeekRepo>().authService }
    single { get<CoffeePeekRepo>().shopApiService }
    single { get<CoffeePeekRepo>().userApiService }
    single { get<CoffeePeekRepo>().photoApiService }
    single { get<CoffeePeekRepo>().reviewApiService }
    single { get<CoffeePeekRepo>().checkInApiService }
    single { get<CoffeePeekRepo>().feedApiService }
    single { get<CoffeePeekRepo>().shopIssueReportApiService }
    single { get<CoffeePeekRepo>().roasterApiService }
    single { get<CoffeePeekRepo>().shopChangeRequestApiService }
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
    if (registerLegacyFavorites) {
        single<FavoriteRepository> { FavoriteRepositoryImpl(get(), get(), get(), get()) }
    }
    single<ShopRepository> { ShopRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get(), get()) }
    single<CheckInRepository> { CheckInRepositoryImpl(get(), get(), get()) }
    single<com.coffeepeek.domain.feature.feed.FeedRepository> { com.coffeepeek.data.feature.feed.FeedRepositoryImpl(get(), get()) }
    single<ShopIssueReportRepository> { ShopIssueReportRepositoryImpl(get()) }
    single<RoasterRepository> { RoasterRepositoryImpl(get(), get(), get(), database.settingRepository, baseUrl) }
    single<ShopChangeRequestRepository> { ShopChangeRequestRepositoryImpl(get(), get()) }
}
