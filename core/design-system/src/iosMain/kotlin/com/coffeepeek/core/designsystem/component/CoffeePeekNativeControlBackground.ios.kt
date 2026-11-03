@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.isAccessibilityElement

@Composable
internal actual fun CoffeePeekNativeControlBackground(
    modifier: Modifier, prominent: Boolean, tint: Color?, enabled: Boolean, pressed: Boolean,
    shape: Shape, hazeState: HazeState?,
) {
    val colors = MaterialTheme.colorScheme
    UIKitView(
        factory = {
            UIButton.buttonWithConfiguration(glassButtonConfiguration(prominent), null).apply {
                userInteractionEnabled = false
                isAccessibilityElement = false
            }
        },
        // Match UIButton's capsule, including when a caller requests a different Android shape.
        modifier = modifier.coffeePeekNativeUnderlayClip(CircleShape),
        // Disabling UIButton alone does not disable the interop host's touch arbitration.
        properties = UIKitInteropProperties(interactionMode = null,
            isNativeAccessibilityEnabled = false, placedAsOverlay = false),
        update = { button ->
            button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            button.backgroundColor = UIColor.clearColor
            button.tintColor = (tint ?: colors.primary).toUIColor()
            button.configuration = glassButtonConfiguration(prominent).apply {
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                if (tint != null) baseBackgroundColor = tint.toUIColor()
            }
            button.enabled = enabled
            button.highlighted = pressed
        },
    )
}
