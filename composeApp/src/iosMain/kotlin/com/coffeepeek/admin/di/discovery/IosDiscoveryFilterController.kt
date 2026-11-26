@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.discovery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.core.designsystem.component.CoffeePeekPresentNativeController
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosFilterPresentation
import org.koin.compose.koinInject
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIPresentationController
import platform.UIKit.UIViewController
import platform.UIKit.presentationController
import platform.darwin.NSObject

internal val LocalIosDiscoveryFilterController =
    staticCompositionLocalOf<(IosDiscoveryFilters) -> UIViewController> {
        error("The iOS app must provide a Discovery filters controller")
    }

@Composable
internal fun IosDiscoveryFilterController(
    presentation: DiscoveryIosFilterPresentation,
    factory: (IosDiscoveryFilters) -> UIViewController,
) {
    val host = LocalUIViewController.current
    val fetcher = koinInject<ImageUrlFetcher>()
    val model = remember(host, fetcher) { IosDiscoveryFilters(presentation, IosCatalogueMedia(fetcher, Constants.BASE_URL)) }
    val controller = remember(host, model, factory) { factory(model) }
    val delegate = remember(model) { FilterDismissDelegate(model::apply) }
    val style = MaterialTheme.colorScheme.uiKitStyle
    SideEffect {
        model.update(presentation)
        controller.overrideUserInterfaceStyle = style
    }
    DisposableEffect(model, controller) {
        controller.presentationController?.delegate = delegate
        onDispose {
            controller.presentationController?.delegate = null
            model.dispose()
        }
    }
    CoffeePeekPresentNativeController(controller, host)
}

private class FilterDismissDelegate(private val onClose: () -> Unit) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) = onClose()
}
