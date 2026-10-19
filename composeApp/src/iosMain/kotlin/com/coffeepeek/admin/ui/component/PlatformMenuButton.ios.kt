@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElementAttributesDisabled
import platform.UIKit.UIMenuElementState
import platform.UIKit.UIMenuOptionsSingleSelection
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityValue

@Composable
internal actual fun PlatformMenuButton(
    items: List<PlatformMenuItem>,
    contentDescription: String,
    modifier: Modifier,
    enabled: Boolean,
    valueDescription: String?,
    content: @Composable (onClick: () -> Unit) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val currentItems = rememberUpdatedState(items)
    Box(modifier) {
        // Keep the shared trigger's appearance; UIKit owns its touch and VoiceOver interaction.
        Box(Modifier.clearAndSetSemantics { }) { content { } }
        UIKitView(
            factory = {
                UIButton.buttonWithType(UIButtonTypeCustom).apply {
                    showsMenuAsPrimaryAction = true
                    backgroundColor = UIColor.clearColor
                }
            },
            modifier = Modifier.matchParentSize(),
            // Overlay avoids a transparent cutout in Compose. The interop container follows
            // this trigger's placement/clipping when a card or form scrolls.
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
            update = { button ->
                button.overrideUserInterfaceStyle = colors.uiKitStyle
                button.backgroundColor = UIColor.clearColor
                button.tintColor = colors.onSurface.toUIColor()
                button.enabled = enabled
                button.accessibilityLabel = contentDescription
                button.accessibilityValue = valueDescription
                button.menu = UIMenu.menuWithTitle(
                    title = "",
                    image = null,
                    identifier = null,
                    options = if (items.any { it.selected != null }) UIMenuOptionsSingleSelection else 0uL,
                    children = items.map { item ->
                        val symbol = when (item.icon) {
                            PlatformMenuIcon.Edit -> "square.and.pencil"
                            PlatformMenuIcon.Hide -> "eye.slash"
                            PlatformMenuIcon.Report -> "flag"
                            PlatformMenuIcon.NoteEdit -> "note.text"
                            PlatformMenuIcon.Add -> "plus"
                            PlatformMenuIcon.CoffeeBean -> "leaf"
                            PlatformMenuIcon.Logout -> "rectangle.portrait.and.arrow.right"
                            PlatformMenuIcon.Block -> "person.crop.circle.badge.xmark"
                            null -> null
                        }
                        UIAction.actionWithTitle(item.title, symbol?.let { UIImage.systemImageNamed(it) }, item.id) {
                            currentItems.value.firstOrNull { it.id == item.id && it.enabled }?.onClick?.invoke()
                        }.apply {
                            attributes = (if (item.enabled) 0uL else UIMenuElementAttributesDisabled) or
                                (if (item.destructive) platform.UIKit.UIMenuElementAttributesDestructive else 0uL)
                            state = if (item.selected == true) UIMenuElementState.UIMenuElementStateOn
                                else UIMenuElementState.UIMenuElementStateOff
                        }
                    },
                )
            },
            onRelease = { it.menu = null },
        )
    }
}
