@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIActivityIndicatorViewStyleLarge
import platform.UIKit.UIActivityIndicatorViewStyleMedium
import platform.UIKit.accessibilityLabel
import platform.UIKit.isAccessibilityElement

@Composable
internal actual fun CoffeePeekPlatformLoader(contentDescription: String, modifier: Modifier, size: Dp, color: Color, strokeWidth: Dp) {
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = { UIActivityIndicatorView().apply { startAnimating() } },
        modifier = modifier.size(size),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = false),
        update = { indicator ->
            indicator.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            indicator.activityIndicatorViewStyle = if (size > 24.dp) UIActivityIndicatorViewStyleLarge else UIActivityIndicatorViewStyleMedium
            indicator.color = color.toUIColor()
            indicator.isAccessibilityElement = true
            indicator.accessibilityLabel = contentDescription
        },
        onRelease = { it.stopAnimating() },
    )
}
