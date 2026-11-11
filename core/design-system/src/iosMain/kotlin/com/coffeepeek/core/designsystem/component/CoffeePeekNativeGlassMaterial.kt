@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import kotlinx.cinterop.useContents
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIVisualEffectView

private var nativeGlassFactory: (() -> UIVisualEffectView)? = null

fun configureCoffeePeekNativeGlass(makeView: () -> UIVisualEffectView) {
    nativeGlassFactory = makeView
}

/** One real material, hosted above Compose together with its native foreground. */
internal fun coffeePeekGlassMaterial(): UIVisualEffectView =
    nativeGlassFactory?.invoke()
        ?: UIVisualEffectView(UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular))

internal fun UIVisualEffectView.updateCoffeePeekGlassCorners() {
    if (effect is UIBlurEffect) {
        layer.cornerRadius = bounds.useContents { size.height / 2.0 }
        clipsToBounds = true
    }
}
