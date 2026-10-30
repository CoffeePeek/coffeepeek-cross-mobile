@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ObjCAction
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

// Auth fields share the Compose design-system shape and surface on both platforms.
@Composable
internal actual fun NativeAuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    password: Boolean,
    newPassword: Boolean,
    next: Boolean,
    error: Boolean,
    keyboardType: KeyboardType,
    modifier: Modifier,
): Boolean = false

@Composable
internal actual fun NativeTimePickerRow(label: String, value: String, onAdjust: (Int) -> Unit): Boolean {
    var shown by remember { mutableStateOf(false) }
    val parts = value.split(':')
    val minutes = (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = { shown = true }) { Text(value) }
    }
    if (shown) NativeDatePicker(label, minutes * 60_000L, null, true, { millis ->
        val selected = ((millis / 60_000) % (24 * 60)).toInt()
        onAdjust(selected - minutes)
    }, { shown = false })
    return true
}

private class DoneTarget(val action: () -> Unit) : NSObject() {
    @ObjCAction fun done() = action()
}

/** Numeric Compose fields have no return key. Use a UIKit toolbar tied to the keyboard layout guide. */
@Composable
internal actual fun NativeKeyboardDoneAccessory(visible: Boolean, onDone: () -> Unit) {
    if (!visible) return
    val controller = LocalUIViewController.current
    val action = rememberUpdatedState(onDone)
    val accessorySpace = LocalKeyboardAccessorySpace.current
    DisposableEffect(controller, accessorySpace) {
        val host = controller.parentViewController?.view ?: controller.view
        val target = DoneTarget { host.endEditing(true); action.value() }
        val toolbar = UIToolbar().apply {
            translatesAutoresizingMaskIntoConstraints = false
            items = listOf(
                UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                UIBarButtonItem(title = "Готово", style = UIBarButtonItemStyle.UIBarButtonItemStyleDone, target = target, action = NSSelectorFromString("done")),
            )
        }
        host.addSubview(toolbar)
        val constraints = listOf(
            toolbar.leadingAnchor.constraintEqualToAnchor(host.safeAreaLayoutGuide.leadingAnchor),
            toolbar.trailingAnchor.constraintEqualToAnchor(host.safeAreaLayoutGuide.trailingAnchor),
            toolbar.bottomAnchor.constraintEqualToAnchor(host.keyboardLayoutGuide.topAnchor),
            toolbar.heightAnchor.constraintEqualToConstant(44.0),
        )
        NSLayoutConstraint.activateConstraints(constraints)
        accessorySpace?.set(target, 44.dp)
        onDispose {
            accessorySpace?.remove(target)
            NSLayoutConstraint.deactivateConstraints(constraints)
            toolbar.removeFromSuperview()
            toolbar.items = null
            target.action // Retain the UIBarButtonItem's weak target until disposal.
        }
    }
}
