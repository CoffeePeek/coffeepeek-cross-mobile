@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert

@Composable
internal actual fun CoffeePeekNativeActionDialog(
    title: String, message: String?, actions: List<CoffeePeekDialogAction>, cancelLabel: String?, onDismiss: () -> Unit,
): Boolean {
    val host = LocalUIViewController.current
    val latest = rememberUpdatedState(actions)
    val dismiss = rememberUpdatedState(onDismiss)
    val colors = MaterialTheme.colorScheme
    val controller = remember(title, message, actions.map { it.label to it.destructive }, cancelLabel) {
        UIAlertController.alertControllerWithTitle(title, message, UIAlertControllerStyleAlert).apply {
            actions.forEachIndexed { index, action ->
                addAction(UIAlertAction.actionWithTitle(action.label,
                    if (action.destructive) UIAlertActionStyleDestructive else UIAlertActionStyleDefault) {
                    latest.value.getOrNull(index)?.takeIf { it.enabled && it.label == action.label }?.onClick?.invoke()
                })
            }
            if (cancelLabel != null) addAction(UIAlertAction.actionWithTitle(cancelLabel, UIAlertActionStyleCancel) { dismiss.value() })
        }
    }
    SideEffect {
        controller.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        controller.view.tintColor = colors.primary.toUIColor()
        actions.forEachIndexed { index, action -> (controller.actions[index] as UIAlertAction).enabled = action.enabled }
    }
    CoffeePeekPresentNativeController(controller, host)
    return true
}
