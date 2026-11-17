package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.component.rememberCheckInDateLabel
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosShopCheckInListController =
    staticCompositionLocalOf<(IosShopCheckInList, String) -> UIViewController> {
        error("The iOS app must provide a shop check-in list controller")
    }

@Composable
internal fun iosShopCheckInListContent(): (String) -> UIViewController {
    val factory = LocalIosShopCheckInListController.current
    val sessions = koinInject<SessionReader>()
    val visibility = koinInject<AuthorVisibility>()
    val accounts = koinInject<ProfileAccountPort>()
    val details = koinInject<ShopDetailsRepository>()
    val votes = koinInject<ShopCheckInVoteRepository>()
    val blockActions = koinInject<AuthorBlockActions>()
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    val formatDate by rememberUpdatedState(rememberCheckInDateLabel())
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return { shopId ->
        val language = languageTag
        factory(IosShopCheckInList(shopId, sessions, visibility, accounts, details, votes, blockActions,
            media, language, { raw -> formatDate(raw) }), language)
    }
}
