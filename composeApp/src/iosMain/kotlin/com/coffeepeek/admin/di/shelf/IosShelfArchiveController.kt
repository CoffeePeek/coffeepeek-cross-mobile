package com.coffeepeek.admin.di.shelf

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
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.impl.IosShelfFeature
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosShelfArchiveController = staticCompositionLocalOf<(IosShelfArchive, String) -> UIViewController> {
    error("The iOS app must provide a shelf archive controller")
}

@Composable
internal fun iosShelfArchiveContent(): () -> UIViewController {
    val factory = LocalIosShelfArchiveController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val feature = koinInject<IosShelfFeature>()
    val sessions = koinInject<SessionReader>()
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    return {
        val archive = IosShelfArchive(feature::archive, SessionShelfAccount(sessions), media, Navigator::popBack,
            { id -> Navigator.navigate(Navigator.Screen.ShelfBag(id)) })
        factory(archive, languageTag)
    }
}
