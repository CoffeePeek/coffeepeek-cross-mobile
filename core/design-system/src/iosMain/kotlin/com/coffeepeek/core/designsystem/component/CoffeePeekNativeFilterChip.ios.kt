@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.UIImageRenderingMode.UIImageRenderingModeAlwaysOriginal
import platform.UIKit.NSDirectionalRectEdgeTrailing
import platform.UIKit.accessibilityLabel

@Composable
internal actual fun CoffeePeekNativeFilterChip(
    label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
    leadingIcon: Painter?, trailingIcon: Painter?,
): Boolean {
    if (leadingIcon != null && trailingIcon != null) return false
    val callback = rememberUpdatedState(onClick)
    val colors = MaterialTheme.colorScheme
    val image = (leadingIcon ?: trailingIcon)?.let { coffeePeekNativeImage(it) }
    CoffeePeekNativeGlassView(
        factory = { UIButton.buttonWithConfiguration(glassButtonConfiguration(),
            UIAction.actionWithHandler { callback.value() }) },
        modifier = modifier.heightIn(min = CoffeePeekDimens.glassControlSize),
        update = { button ->
            button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            button.backgroundColor = UIColor.clearColor
            button.tintColor = colors.primary.toUIColor()
            button.configuration = glassButtonConfiguration(prominent = selected).apply {
                title = label
                this.image = image?.imageWithTintColor(
                    (if (selected) colors.onPrimary else colors.primary).toUIColor(), UIImageRenderingModeAlwaysOriginal)
                imagePadding = 6.0
                if (trailingIcon != null) imagePlacement = NSDirectionalRectEdgeTrailing
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                baseForegroundColor = (if (selected) colors.onPrimary else colors.onSurface).toUIColor()
                if (selected) baseBackgroundColor = colors.primary.toUIColor()
                if (coffeePeekLiquidGlassAvailable) automaticallyUpdateForSelection = false
            }
            button.selected = selected
            button.accessibilityLabel = label
            button.titleLabel?.adjustsFontForContentSizeCategory = true
        },
    )
    return true
}
