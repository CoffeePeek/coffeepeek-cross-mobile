package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.IosShelfFeature
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private var nativeShelfFactory: ((IosShelfScreen) -> UIViewController)? = null

fun configureNativeShelf(factory: (IosShelfScreen) -> UIViewController) { nativeShelfFactory = factory }
internal fun nativeShelfController(screen: IosShelfScreen): UIViewController =
    checkNotNull(nativeShelfFactory) { "The iOS app must configure the native shelf" }(screen)

internal typealias IosShelfFactory = (String, String?, Boolean, () -> Boolean) -> IosShelfScreen

internal fun iosShelfFactory(feature: IosShelfFeature, profiles: ShelfProfileRepository, sessions: SessionReader,
    checkIns: FeatureCheckInCreateFlowRenderer, language: LanguagePort, media: IosCatalogueMedia,
): IosShelfFactory = { kind, slug, own, allowed ->
    IosShelfScreen(kind, language.current().languageTag, feature, profiles, slug, own, SessionShelfAccount(sessions), media, checkIns,
        allowed, language, sessions)
}

/** Composition supplies feature factories and cross-feature Home integration; business code stays in feature/shelf. */
@Composable
internal fun iosShelfFactory(): (String, String?, Boolean, () -> Boolean) -> IosShelfScreen {
    val feature = koinInject<IosShelfFeature>()
    val profiles = koinInject<ShelfProfileRepository>()
    val sessions = koinInject<SessionReader>()
    val checkIns = koinInject<FeatureCheckInCreateFlowRenderer>()
    val languagePort = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val media = remember(fetcher) { IosCatalogueMedia(fetcher, Constants.BASE_URL) }
    val language by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    return { kind, slug, own, allowed ->
        IosShelfScreen(kind, language, feature, profiles, slug, own, SessionShelfAccount(sessions), media, checkIns,
            allowed, languagePort, sessions)
    }
}

@Composable
internal fun iosShelfContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val create = iosShelfFactory()
    return { screen -> when (screen) {
        Navigator.Screen.ShelfAddBag -> ({ nativeShelfController(create("add", null, true) { true }) })
        is Navigator.Screen.ShelfBag -> ({ nativeShelfController(create("bag", screen.bagId, true) { true }) })
        else -> null
    } }
}
