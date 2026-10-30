@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeCustom
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIButtonConfigurationCornerStyleCapsule
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIMenu
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.accessibilityLabel

@Composable
internal actual fun OwnershipProfileMenu(
    editLabel: String?, representativeLabel: String, actionsLabel: String,
    onEdit: () -> Unit, onRepresentative: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val currentEdit = rememberUpdatedState(onEdit)
    val currentRepresentative = rememberUpdatedState(onRepresentative)
    UIKitView(
        factory = {
            UIButton.buttonWithType(UIButtonTypeCustom).apply { showsMenuAsPrimaryAction = true }
        },
        modifier = Modifier.size(CoffeePeekDimens.buttonHeight),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
        update = { button ->
            button.overrideUserInterfaceStyle = if (colors.background.luminance() < .5f)
                UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
            val modern = (UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0) >= 26
            val configuration = if (modern) UIButtonConfiguration.glassButtonConfiguration()
                else UIButtonConfiguration.grayButtonConfiguration()
            configuration.cornerStyle = UIButtonConfigurationCornerStyleCapsule
            configuration.image = UIImage.systemImageNamed("ellipsis")
            val tint = colors.onSurface
            configuration.baseForegroundColor = UIColor.colorWithRed(
                tint.red.toDouble(), tint.green.toDouble(), tint.blue.toDouble(), tint.alpha.toDouble())
            button.configuration = configuration
            button.accessibilityLabel = actionsLabel
            button.menu = UIMenu.menuWithTitle(title = "", image = null, identifier = null, options = 0uL,
                children = buildList {
                    if (editLabel != null) add(UIAction.actionWithTitle(editLabel,
                        UIImage.systemImageNamed("square.and.pencil"), "edit") { currentEdit.value() })
                    add(UIAction.actionWithTitle(representativeLabel,
                        UIImage.systemImageNamed("person.crop.circle"), "representative") { currentRepresentative.value() })
                })
        },
        onRelease = { it.menu = null },
    )
}
