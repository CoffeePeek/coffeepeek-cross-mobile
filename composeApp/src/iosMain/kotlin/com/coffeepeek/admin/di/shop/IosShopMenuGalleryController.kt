package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createIosShopMenuGallerySession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosShopMenuGalleryController =
    staticCompositionLocalOf<(IosShopMenuGallery, String) -> UIViewController> {
        error("The iOS app must provide a shop menu gallery controller")
    }

@Composable
internal fun iosShopMenuGalleryContent(): (String) -> UIViewController {
    val factory = LocalIosShopMenuGalleryController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val client = koinInject<ApiClients>().client
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val repository = remember(client) { createShopMenuGalleryRepository(client) }
    return { shopId ->
        val session = createIosShopMenuGallerySession(shopId, repository)
        val gallery = IosShopMenuGallery(session, imageFetcher, Constants.BASE_URL, Navigator::popBack)
        factory(gallery, languageTag)
    }
}
