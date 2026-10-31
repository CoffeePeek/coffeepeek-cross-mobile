@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch
import platform.darwin.NSObject

@Composable
internal actual fun CoffeePeekNativeSwitch(
    checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean,
): Boolean {
    val callback = rememberUpdatedState(onCheckedChange)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    val target = remember { CoffeePeekSwitchTarget { value -> if (active.value) callback.value?.invoke(value) } }
    CoffeePeekNativeGlassView(
        factory = { UISwitch().apply {
            addTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
        } },
        // UIKit determines its width: the iOS 26 switch is wider than the old 51-point control.
        modifier = modifier.heightIn(min = 48.dp),
        interactionMode = if (onCheckedChange != null) UIKitInteropInteractionMode.Cooperative() else null,
        accessibilityEnabled = onCheckedChange != null,
        update = { control ->
            control.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            control.onTintColor = colors.primary.toUIColor()
            control.setOn(checked, animated = false)
            control.enabled = enabled
            control.userInteractionEnabled = enabled && onCheckedChange != null
        },
        onRelease = { it.removeTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged) },
    )
    return true
}

private class CoffeePeekSwitchTarget(val callback: (Boolean) -> Unit) : NSObject() {
    @ObjCAction fun changed(sender: UISwitch) = callback(sender.on)
}
