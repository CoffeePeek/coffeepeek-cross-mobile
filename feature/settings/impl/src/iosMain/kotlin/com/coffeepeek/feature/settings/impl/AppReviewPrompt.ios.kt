@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import platform.Foundation.NSUserDefaults
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.transitionCoordinator

@Composable
internal actual fun rememberAppReviewPlatform(): AppReviewPlatform? {
    val controller = LocalUIViewController.current
    return remember(controller) {
        val defaults = NSUserDefaults.standardUserDefaults
        AppReviewPlatform(
            read = { defaults.stringForKey(AppReviewUsageKey) },
            save = { defaults.setObject(it, forKey = AppReviewUsageKey) },
            canPresent = { controller.reviewScene() != null },
            request = { claim ->
                controller.reviewScene()?.let { scene ->
                    if (claim()) SKStoreReviewController.requestReviewInScene(scene)
                }
            },
        )
    }
}

private fun UIViewController.reviewScene(): UIWindowScene? {
    val window = viewIfLoaded?.window ?: return null
    val scene = window.windowScene ?: return null
    if (!window.isKeyWindow() || scene.activationState != UISceneActivationStateForegroundActive) return null
    val parents = generateSequence(this) { it.parentViewController }
    return scene.takeIf { parents.none { it.presentedViewController != null || it.transitionCoordinator() != null } }
}
