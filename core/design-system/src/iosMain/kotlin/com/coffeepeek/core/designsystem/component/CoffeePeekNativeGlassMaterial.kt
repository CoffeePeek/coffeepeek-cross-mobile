@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import kotlinx.cinterop.useContents
import platform.UIKit.UIBackgroundConfiguration
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIVisualEffectView

private var nativeGlassFactory: (() -> UIVisualEffectView)? = null

fun configureCoffeePeekNativeGlass(makeView: () -> UIVisualEffectView) {
    nativeGlassFactory = makeView
}

internal val coffeePeekLiquidGlassAvailable: Boolean
    get() = (UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0) >= 26

fun coffeePeekSystemMaterial(): UIBlurEffect =
    UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleSystemMaterial)

fun coffeePeekGlassButtonConfiguration(prominent: Boolean = false): UIButtonConfiguration {
    if (coffeePeekLiquidGlassAvailable) {
        return if (prominent) UIButtonConfiguration.prominentGlassButtonConfiguration()
        else UIButtonConfiguration.glassButtonConfiguration()
    }
    if (prominent) return UIButtonConfiguration.filledButtonConfiguration()
    return UIButtonConfiguration.plainButtonConfiguration().apply {
        background = UIBackgroundConfiguration.clearConfiguration().apply {
            backgroundColor = UIColor.clearColor
            visualEffect = coffeePeekSystemMaterial()
        }
    }
}

/** One real material, hosted above Compose together with its native foreground. */
internal fun coffeePeekGlassMaterial(): UIVisualEffectView =
    nativeGlassFactory?.invoke()
        ?: UIVisualEffectView(coffeePeekSystemMaterial())

internal fun UIVisualEffectView.updateCoffeePeekGlassCorners() {
    if (effect is UIBlurEffect) {
        layer.cornerRadius = bounds.useContents { size.height / 2.0 }
        clipsToBounds = true
    }
}
