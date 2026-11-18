@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.ownership

import androidx.compose.runtime.*
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.ownership.impl.LocalIosOwnershipContent
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var ownershipFactory: (IosOwnershipScreen) -> UIViewController
internal fun configureNativeOwnership(factory: (IosOwnershipScreen) -> UIViewController) { ownershipFactory = factory }

@Composable
internal actual fun ProvidePlatformOwnership(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIosOwnershipContent provides { session ->
        val language = koinInject<LanguagePort>()
        val fetcher = koinInject<ImageUrlFetcher>()
        val screen = remember(session) { IosOwnershipScreen(session, language, IosCatalogueMedia(fetcher, Constants.BASE_URL)) }
        val controller = remember(screen) { ownershipFactory(screen) }
        val theme by ThemeManager.themeMode.collectAsState()
        SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
        DisposableEffect(screen) { onDispose { screen.close() } }
        controller
    }, content = content)
}
