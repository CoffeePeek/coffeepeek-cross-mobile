@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.accessibilityLabel

@Composable
internal actual fun CoffeePeekNativePrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
): Boolean {
    val callback = rememberUpdatedState(onClick)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    CoffeePeekNativeGlassView(
        factory = { UIButton.buttonWithConfiguration(coffeePeekGlassButtonConfiguration(prominent = true),
            UIAction.actionWithHandler { if (active.value) callback.value() }) },
        modifier = modifier,
        update = { button ->
            button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            button.configuration = coffeePeekGlassButtonConfiguration(prominent = true).apply {
                title = text
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                baseBackgroundColor = colors.primary.toUIColor()
                baseForegroundColor = colors.onPrimary.toUIColor()
            }
            button.accessibilityLabel = text
            button.titleLabel?.adjustsFontForContentSizeCategory = true
            button.titleLabel?.numberOfLines = 0
            button.enabled = enabled
        },
    )
    return true
}
