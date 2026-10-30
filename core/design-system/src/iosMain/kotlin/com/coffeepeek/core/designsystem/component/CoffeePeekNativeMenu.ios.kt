@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_actions
import org.jetbrains.compose.resources.stringResource
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIColor
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElementAttributesDestructive
import platform.UIKit.accessibilityLabel

@Composable
internal actual fun CoffeePeekNativeMenu(
    actions: List<CoffeePeekMenuAction>, modifier: Modifier, content: @Composable (open: () -> Unit) -> Unit,
): Boolean {
    val latest = rememberUpdatedState(actions)
    val colors = MaterialTheme.colorScheme
    val description = stringResource(Res.string.coffeepeek_actions)
    Box(modifier) {
        Box(Modifier.clearAndSetSemantics { }) { content { } }
        UIKitView(
            factory = { UIButton.buttonWithType(UIButtonTypeCustom).apply {
                showsMenuAsPrimaryAction = true
                backgroundColor = UIColor.clearColor
            } },
            modifier = Modifier.matchParentSize(),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
            update = { button ->
                button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
                button.accessibilityLabel = description
                button.menu = UIMenu.menuWithTitle("", null, null, 0uL,
                    actions.mapIndexed { index, action ->
                        UIAction.actionWithTitle(action.label, null, index.toString()) {
                            latest.value.getOrNull(index)?.takeIf { it.label == action.label }?.onClick?.invoke()
                        }.apply {
                            if (action.destructive) attributes = UIMenuElementAttributesDestructive
                        }
                    })
            },
            onRelease = { it.menu = null },
        )
    }
    return true
}
