@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_actions
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElementAttributesDestructive
import platform.UIKit.accessibilityLabel

@Composable
internal actual fun CoffeePeekNativeMenu(
    actions: List<CoffeePeekMenuAction>, modifier: Modifier, enabled: Boolean,
    content: @Composable (open: () -> Unit) -> Unit,
): Boolean {
    val latest = rememberUpdatedState(actions)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    val description = stringResource(Res.string.coffeepeek_actions)
    val images = actions.map { coffeePeekNativeImage(it.icon) }
    Box(modifier) {
        Box(Modifier.clearAndSetSemantics { }) { content { } }
        CoffeePeekNativeGlassView(
            factory = { UIButton.buttonWithType(UIButtonTypeCustom).apply {
                showsMenuAsPrimaryAction = true
                backgroundColor = UIColor.clearColor
            } },
            modifier = Modifier.matchParentSize(),
            update = { button ->
                button.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
                button.tintColor = colors.primary.toUIColor()
                button.accessibilityLabel = description
                button.enabled = enabled
                button.menu = coffeePeekActionMenu(actions, images) { if (active.value) latest.value else emptyList() }
            },
            onRelease = { it.menu = null },
        )
    }
    return true
}

private fun coffeePeekActionMenu(
    actions: List<CoffeePeekMenuAction>, images: List<UIImage?>, latest: () -> List<CoffeePeekMenuAction>,
): UIMenu = UIMenu.menuWithTitle("", null, null, 0uL, actions.mapIndexed { index, action ->
    UIAction.actionWithTitle(action.label, images[index], index.toString()) {
        latest().getOrNull(index)?.takeIf { it.label == action.label }?.onClick?.invoke()
    }.apply {
        if (action.destructive) attributes = UIMenuElementAttributesDestructive
    }
})
