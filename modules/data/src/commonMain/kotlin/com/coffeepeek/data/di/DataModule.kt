package com.coffeepeek.data.di

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.api.CoffeePeekRepo
import com.coffeepeek.data.session.SessionTokenProvider
import com.coffeepeek.core.session.DefaultSessionStore
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.core.session.createPlatformSessionSecureStore
import com.coffeepeek.data.session.RoomLegacyCredentialRows
import com.coffeepeek.data.repository.AuthRepositoryImpl
import com.coffeepeek.data.repository.CheckInRepositoryImpl
import com.coffeepeek.data.repository.FavoriteRepositoryImpl
import com.coffeepeek.data.repository.PhotoRepositoryImpl
import com.coffeepeek.data.repository.ReviewRepositoryImpl
import com.coffeepeek.data.repository.RoasterRepositoryImpl
import com.coffeepeek.data.repository.SessionRepositoryImpl
import com.coffeepeek.data.repository.ShopChangeRequestRepositoryImpl
import com.coffeepeek.data.repository.ShopIssueReportRepositoryImpl
import com.coffeepeek.data.repository.ShopRepositoryImpl
import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopIssueReportRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.coffeepeek.room.DatabaseCore
import org.koin.core.module.Module
import org.koin.dsl.module

fun dataModule(
    baseUrl: String,
    cacheFolderPath: String,
    appCacheRootPath: String,
    database: DatabaseCore,
    platformContext: Any? = null,
    debug: Boolean = false,
    registerLegacyFavorites: Boolean = true,
): Module = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

    single<com.coffeepeek.core.session.LegacyCredentialRows> { RoomLegacyCredentialRows(database.settingRepository) }
    single { createPlatformSessionSecureStore(platformContext, get()) }
    single<SessionRepository> { SessionRepositoryImpl(DefaultSessionStore(get(), get())) }
    single<SessionStore> { get<SessionRepository>() }
    single<SessionReader> { get<SessionRepository>() }
    single { SessionTokenProvider(get(), get()) }
    single { FileUrlResolver(baseUrl) }
    single {
        UserSessionCleaner(
            sessionRepository = get(),
            favoriteRepository = get(),
            httpCacheFolderPath = cacheFolderPath,
            appCacheRootPath = appCacheRootPath,
        )
    }

    single {
        val tokenProvider = get<SessionTokenProvider>()
        CoffeePeekClient(
            url = baseUrl,
            cacheFolderPath = cacheFolderPath,
            debug = debug,
            getToken = { tokenProvider.current() },
            saveToken = { authResp ->
                val sessionRepository = get<SessionRepository>()
                val current = sessionRepository.peekSession()
                val session = authResp?.let {
                    com.coffeepeek.domain.model.Session(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken.takeIf { token -> token.isNotBlank() }
                            ?: current?.refreshToken,
                        userId = JwtUtils.extractUserId(it.accessToken) ?: current?.userId,
                    )
                }
                sessionRepository.saveSession(session)
            },
            replaceToken = { expected, updated ->
                val sessions = get<SessionRepository>()
                val old = sessions.peekSession()
                if (old?.accessToken != expected.accessToken || old.refreshToken.orEmpty() != expected.refreshToken) false
                else sessions.replaceSession(old, updated?.let {
                    com.coffeepeek.domain.model.Session(it.accessToken, it.refreshToken,
                        JwtUtils.extractUserId(it.accessToken) ?: old.userId)
                })
            },
        )
    }

    single { CoffeePeekRepo(get()) }
    single { get<CoffeePeekRepo>().authService }
    single { get<CoffeePeekRepo>().shopApiService }
    single { get<CoffeePeekRepo>().favoritesApiService }
    single { get<CoffeePeekRepo>().userApiService }
    single { get<CoffeePeekRepo>().photoApiService }
    single { get<CoffeePeekRepo>().reviewApiService }
    single { get<CoffeePeekRepo>().checkInApiService }
    single { get<CoffeePeekRepo>().feedApiService }
    single { get<CoffeePeekRepo>().shopIssueReportApiService }
    single { get<CoffeePeekRepo>().roasterApiService }
    single { get<CoffeePeekRepo>().shopChangeRequestApiService }
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<com.coffeepeek.domain.feature.favorites.api.FavoritesRepository> {
        com.coffeepeek.data.feature.favorites.ServerFavoritesRepository(get(), get())
    }
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
