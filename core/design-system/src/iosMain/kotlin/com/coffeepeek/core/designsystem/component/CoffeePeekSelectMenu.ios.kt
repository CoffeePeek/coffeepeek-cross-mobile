@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIColor
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElementState
import platform.UIKit.UIMenuOptionsSingleSelection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityValue

@Composable
internal actual fun CoffeePeekSelectMenu(
    options: List<CoffeePeekSelectOption>, contentDescription: String, valueDescription: String,
    errorDescription: String?, modifier: Modifier, enabled: Boolean,
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val currentOptions = rememberUpdatedState(options)
    val currentEnabled = rememberUpdatedState(enabled)
    Box(modifier) {
        // UIKit owns touch and VoiceOver; the Compose button supplies the shared appearance.
        Box(Modifier.clearAndSetSemantics { }) { content { } }
        UIKitView(
            factory = {
                UIButton.buttonWithType(UIButtonTypeCustom).apply {
                    showsMenuAsPrimaryAction = true
                    backgroundColor = UIColor.clearColor
                }
            },
            modifier = Modifier.matchParentSize(),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
            update = { button ->
                button.overrideUserInterfaceStyle = if (colors.background.luminance() < .5f)
                    UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
                val tint = colors.onSurface
                button.tintColor = UIColor.colorWithRed(tint.red.toDouble(), tint.green.toDouble(), tint.blue.toDouble(), tint.alpha.toDouble())
                button.enabled = enabled
                button.accessibilityLabel = contentDescription
                button.accessibilityValue = valueDescription
                button.accessibilityHint = errorDescription
                button.menu = UIMenu.menuWithTitle(
                    title = "", image = null, identifier = null, options = UIMenuOptionsSingleSelection,
                    children = options.map { option ->
                        UIAction.actionWithTitle(option.label, null, option.id) {
                            if (currentEnabled.value) currentOptions.value.firstOrNull { it.id == option.id }?.onSelect?.invoke()
                        }.apply {
                            state = if (option.selected) UIMenuElementState.UIMenuElementStateOn else UIMenuElementState.UIMenuElementStateOff
                        }
                    },
                )
            },
            onRelease = { it.menu = null },
        )
    }
}
