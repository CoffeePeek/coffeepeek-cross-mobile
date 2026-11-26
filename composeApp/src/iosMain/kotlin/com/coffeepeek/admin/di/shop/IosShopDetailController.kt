package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.*
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

object IosShopDetailControllers {
    private lateinit var factory: (IosShopDetailRoute) -> UIViewController
    fun configure(factory: (IosShopDetailRoute) -> UIViewController) { this.factory = factory }
    internal fun controller(route: IosShopDetailRoute) = factory(route)
}

@Composable
internal fun iosShopDetailContent(): (String, Boolean) -> UIViewController {
    val accounts = koinInject<SessionReader>()
    val profiles = koinInject<ProfileAccountPort>()
    val visibility = koinInject<AuthorVisibility>()
    val details = koinInject<ShopDetailsRepository>()
    val votes = koinInject<ShopCheckInVoteRepository>()
    val favorites = koinInject<FavoritesRepository>()
    val favoriteIntent = koinInject<ShopFavoriteIntent>()
    val collections = koinInject<FavoriteCollectionsPrompt>()
    val blocks = koinInject<AuthorBlockActions>()
    val forms = koinInject<ShopCheckInCreateEntry>() as SessionShopCheckInEntry
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val ownership = koinInject<BusinessOwnershipEntry>()
    val factory = remember(accounts, profiles, visibility, details, votes, favorites, favoriteIntent, collections, blocks, forms, language, fetcher, ownership) {
        IosNativeShopDetailFactory(accounts, profiles, visibility, details, votes, favorites, favoriteIntent, collections,
            blocks, forms, language, fetcher, ownership)
    }
    return factory::controller
}
