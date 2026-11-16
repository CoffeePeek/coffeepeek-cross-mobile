package com.coffeepeek.admin.di.coffee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.createIosCoffeeDetailSession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosCoffeeDetailController =
    staticCompositionLocalOf<(IosCoffeeDetail, String) -> UIViewController> {
        error("The iOS app must provide a coffee detail controller")
    }

@Composable
internal fun iosCoffeeDetailContent(): (String) -> UIViewController {
    val factory = LocalIosCoffeeDetailController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val repository = koinInject<CoffeeRepository>()
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    return { slug ->
        val session = createIosCoffeeDetailSession(slug, repository, CoffeeCatalogueAnalytics.detail())
        factory(IosCoffeeDetail(session, media), languageTag)
    }
}
