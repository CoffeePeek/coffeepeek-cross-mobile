@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.utils

import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

actual object ShareHelper {
    actual fun shareText(text: String) {
        val presenter = topViewController() ?: return
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        controller.overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
        controller.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
            permittedArrowDirections = 0u
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }
}

private fun topViewController(): UIViewController? {
    var current = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (current?.presentedViewController != null) {
        current = current.presentedViewController
    }
    return current
}
