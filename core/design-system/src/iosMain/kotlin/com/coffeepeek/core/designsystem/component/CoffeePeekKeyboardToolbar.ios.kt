@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.ColorScheme
import platform.UIKit.UIToolbar
import platform.UIKit.UIToolbarAppearance

/** Keep the keyboard accessory opaque even when its input lives in a Compose dialog. */
fun UIToolbar.coffeePeekApplyKeyboardColors(colors: ColorScheme) {
    val appearance = UIToolbarAppearance().apply {
        configureWithOpaqueBackground()
        backgroundColor = colors.background.toUIColor()
    }
    standardAppearance = appearance
    compactAppearance = appearance
    scrollEdgeAppearance = appearance
    compactScrollEdgeAppearance = appearance
    translucent = false
    backgroundColor = colors.background.toUIColor()
    tintColor = colors.onSurface.toUIColor()
    overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
}
