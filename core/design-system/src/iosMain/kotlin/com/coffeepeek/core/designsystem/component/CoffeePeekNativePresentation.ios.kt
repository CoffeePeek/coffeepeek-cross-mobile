@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIViewController
import platform.UIKit.endEditing
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
        // A visible page sheet may keep a transition coordinator while its custom detent
        // resizes. Its child sheet can be presented without waiting for that resize.
        while (host.view.window == null ||
            (host.presentingViewController == null && host.transitionCoordinator != null)) delay(16)
        var presenter = host.coffeePeekPresentationHost()
        while (presenter.isBeingDismissed() ||
            (presenter.transitionCoordinator != null &&
                (presenter !== host || host.presentingViewController == null))) {
            delay(16)
            presenter = host.coffeePeekPresentationHost()
        }
        presenter.view.endEditing(true)
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
