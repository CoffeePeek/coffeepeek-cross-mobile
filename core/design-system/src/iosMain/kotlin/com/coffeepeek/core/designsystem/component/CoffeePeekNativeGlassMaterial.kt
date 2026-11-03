@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import kotlinx.cinterop.useContents
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UICornerConfiguration
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIVisualEffectView

/** One real material, hosted above Compose together with its native foreground. */
internal fun coffeePeekGlassMaterial(): UIVisualEffectView {
    val effect = if (coffeePeekLiquidGlassAvailable) {
        UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
    } else UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleRegular)
    return UIVisualEffectView(effect).apply {
        if (coffeePeekLiquidGlassAvailable) cornerConfiguration = UICornerConfiguration.capsuleConfiguration()
    }
}

internal fun UIVisualEffectView.updateCoffeePeekGlassCorners() {
    if (!coffeePeekLiquidGlassAvailable) {
        layer.cornerRadius = bounds.useContents { size.height / 2.0 }
        clipsToBounds = true
    }
}
