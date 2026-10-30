@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIMenu
import platform.UIKit.accessibilityLabel

data class CoffeePeekNativeMenuAction(
    val id: String,
    val label: String,
    val systemSymbolName: String,
    val onClick: () -> Unit,
)

@Composable
fun CoffeePeekNativeActionMenu(
    actions: List<CoffeePeekNativeMenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val currentActions = rememberUpdatedState(actions)
    UIKitView(
        factory = {
            UIButton.buttonWithType(UIButtonTypeCustom).apply {
                showsMenuAsPrimaryAction = true
                backgroundColor = UIColor.clearColor
            }
        },
        modifier = modifier,
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { button ->
            button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            button.configuration = glassButtonConfiguration().apply {
                cornerStyle = UIButtonConfigurationCornerStyleCapsule
                image = UIImage.systemImageNamed("ellipsis")
                baseForegroundColor = colors.onSurface.toUIColor()
            }
            button.accessibilityLabel = contentDescription
            button.menu = UIMenu.menuWithTitle(
                title = "", image = null, identifier = null, options = 0uL,
                children = actions.map { action ->
                    UIAction.actionWithTitle(
                        action.label, UIImage.systemImageNamed(action.systemSymbolName), action.id,
                    ) {
                        currentActions.value.firstOrNull { it.id == action.id }?.onClick?.invoke()
                    }
                },
            )
        },
        onRelease = { it.menu = null },
    )
}
