@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIViewController
import platform.UIKit.transitionCoordinator

internal fun UIViewController.coffeePeekPresentationHost(): UIViewController {
    var host = this
    while (host.presentedViewController != null && !host.presentedViewController!!.isBeingDismissed()) {
        host = host.presentedViewController!!
    }
    return host
}

@Composable
internal fun CoffeePeekPresentNativeController(controller: UIViewController, host: UIViewController) {
    LaunchedEffect(controller, host) {
        while (host.view.window == null || host.transitionCoordinator != null) delay(16)
        var presenter = host.coffeePeekPresentationHost()
        while (presenter.isBeingDismissed() || presenter.transitionCoordinator != null) {
            delay(16)
            presenter = host.coffeePeekPresentationHost()
        }
        presenter.presentViewController(controller, !UIAccessibilityIsReduceMotionEnabled(), null)
    }
    DisposableEffect(controller) {
        onDispose {
            if (controller.presentingViewController != null && !controller.isBeingDismissed()) {
                controller.dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled(), null)
            }
        }
    }
}
