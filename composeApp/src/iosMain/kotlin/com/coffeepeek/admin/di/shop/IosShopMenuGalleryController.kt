package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.shop.data.repository.createShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.createIosShopMenuGallerySession
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
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
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    val repository = remember(client) { createShopMenuGalleryRepository(client) }
    return iosShopMenuGalleryContent(factory, { languageTag }, repository, media)
}

internal fun iosShopMenuGalleryContent(factory: (IosShopMenuGallery, String) -> UIViewController,
    language: () -> String, repository: ShopMenuGalleryRepository, media: IosCatalogueMedia,
): (String) -> UIViewController = { shopId ->
        val session = createIosShopMenuGallerySession(shopId, repository)
        val gallery = IosShopMenuGallery(session, media, Navigator::popBack)
        factory(gallery, language())
}
