@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.PresentNativeController
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
internal actual fun ShopRoutePicker(latitude: Double, longitude: Double, onDismiss: () -> Unit) {
    val host = LocalUIViewController.current
    val dismiss = rememberUpdatedState(onDismiss)
    val controller = remember(latitude, longitude, host) {
        UIActivityViewController(
            activityItems = listOf(NSURL(string = buildAppleMapsRouteUrl(latitude, longitude))),
            applicationActivities = null,
        ).apply {
            completionWithItemsHandler = { _, _, _, _ -> dismiss.value() }
            popoverPresentationController?.apply {
                sourceView = host.view
                sourceRect = host.view.bounds
                permittedArrowDirections = 0u
            }
        }
    }
    SideEffect { controller.overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle }
    PresentNativeController(controller, host)
}
