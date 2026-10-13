@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.theme.switchPalette
import kotlinx.cinterop.ObjCAction
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlinx.coroutines.delay

internal fun UIViewController.presentationHost(): UIViewController {
    var host = this
    while (host.presentedViewController != null && !host.presentedViewController!!.isBeingDismissed()) {
        host = host.presentedViewController!!
    }
    return host
}

/** Presentation can be requested by state before its screen's push animation has finished. */
@Composable
private fun PresentNativeController(controller: UIViewController, host: UIViewController) {
    LaunchedEffect(controller, host) {
        while (host.view.window == null || host.transitionCoordinator != null) delay(16)
        var presenter = host.presentationHost()
        while (presenter.isBeingDismissed() || presenter.transitionCoordinator != null) {
            delay(16)
            presenter = host.presentationHost()
        }
        presenter.presentViewController(controller, !UIAccessibilityIsReduceMotionEnabled(), null)
    }
    DisposableEffect(controller) {
        onDispose {
            if (controller.presentingViewController != null) controller.dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled(), null)
        }
    }
}

@Composable
internal actual fun PlatformAlert(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean, enabled: Boolean) {
    val host = LocalUIViewController.current
    val confirm = rememberUpdatedState(onConfirm)
    val dismiss = rememberUpdatedState(onDismiss)
    val alert = remember(title, message, confirmLabel, cancelLabel, destructive) {
        UIAlertController.alertControllerWithTitle(title, message, UIAlertControllerStyleAlert)
    }
    DisposableEffect(alert, host) {
        alert.addAction(UIAlertAction.actionWithTitle(confirmLabel, if (destructive) UIAlertActionStyleDestructive else UIAlertActionStyleDefault) { confirm.value() })
        cancelLabel?.let { alert.addAction(UIAlertAction.actionWithTitle(it, UIAlertActionStyleCancel) { dismiss.value() }) }
        onDispose { }
    }
    PresentNativeController(alert, host)
    SideEffect { (alert.actions.firstOrNull() as? UIAlertAction)?.enabled = enabled }
}

@Composable
internal actual fun NativeActionSheet(title: String, actions: List<PlatformAction>, onDismiss: () -> Unit): Boolean {
    val host = LocalUIViewController.current
    val latestActions = rememberUpdatedState(actions)
    val dismiss = rememberUpdatedState(onDismiss)
    val alert = remember(host, title) {
        val presenter = host.presentationHost()
        val alert = UIAlertController.alertControllerWithTitle(title, null, UIAlertControllerStyleActionSheet)
        actions.forEachIndexed { index, action ->
            alert.addAction(UIAlertAction.actionWithTitle(action.title, if (action.destructive) UIAlertActionStyleDestructive else UIAlertActionStyleDefault) {
                // Run after dismissal: the next action may present the photo picker.
                val selected = latestActions.value.getOrNull(index)
                alert.dismissViewControllerAnimated(true) { dismiss.value(); selected?.onClick?.invoke() }
            }.apply { enabled = action.enabled })
        }
        alert.addAction(UIAlertAction.actionWithTitle("Отмена", UIAlertActionStyleCancel) { dismiss.value() })
        alert.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
            permittedArrowDirections = 0u
        }
        alert
    }
    PresentNativeController(alert, host)
    return true
}

private class SheetDelegate(val dismissed: () -> Unit) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) = dismissed()
}

@Composable
internal actual fun NativeModalSheet(onDismiss: () -> Unit, dismissEnabled: Boolean, content: @Composable ColumnScope.() -> Unit): Boolean {
    val host = LocalUIViewController.current
    val dismiss = rememberUpdatedState(onDismiss)
    val body = rememberUpdatedState(content)
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val controller = remember {
        ComposeUIViewController {
            AppContent {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(top = 24.dp).dismissKeyboardOnBackgroundGestures()) { body.value(this) }
                }
            }
        }.apply { modalPresentationStyle = UIModalPresentationPageSheet }
    }
    val delegate = remember { SheetDelegate { dismiss.value() } }
    SideEffect {
        controller.modalInPresentation = !dismissEnabled
        controller.overrideUserInterfaceStyle = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }
    DisposableEffect(controller, host) {
        controller.sheetPresentationController?.apply {
            detents = listOf(UISheetPresentationControllerDetent.mediumDetent(), UISheetPresentationControllerDetent.largeDetent())
            prefersGrabberVisible = true
            prefersScrollingExpandsWhenScrolledToEdge = false
        }
        controller.presentationController?.delegate = delegate
        onDispose {
            controller.presentationController?.delegate = null
        }
    }
    PresentNativeController(controller, host)
    return true
}

private class ControlTarget(var changed: (UIControl) -> Unit) : NSObject() {
    @ObjCAction fun change(sender: UIControl) = changed(sender)
}

@Composable
internal actual fun PlatformSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean, modifier: Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val palette = switchPalette(dark)
    val callback = rememberUpdatedState(onCheckedChange)
    val target = remember { ControlTarget { control -> platformHaptic(HapticEvent.Selection); callback.value?.invoke((control as UISwitch).on) } }
    UIKitView(factory = { UISwitch().apply { addTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged) } },
        modifier = modifier.size(51.dp, 44.dp),
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = false, interactionMode = UIKitInteropInteractionMode.NonCooperative),
        update = {
            it.overrideUserInterfaceStyle = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
            val thumb = if (checked) palette.checkedThumb else palette.uncheckedThumb
            it.thumbTintColor = UIColor.colorWithRed(thumb.red.toDouble(), thumb.green.toDouble(), thumb.blue.toDouble(), 1.0)
            val track = palette.checkedTrack
            it.onTintColor = UIColor.colorWithRed(track.red.toDouble(), track.green.toDouble(), track.blue.toDouble(), 1.0)
            it.setOn(checked, false)
            it.enabled = enabled
            it.userInteractionEnabled = onCheckedChange != null
        },
        onRelease = { it.removeTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged) })
}

@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier): Boolean {
    if (labels.size !in 2..4) return false
    val callback = rememberUpdatedState(onSelected)
    val target = remember { ControlTarget { platformHaptic(HapticEvent.Selection); callback.value((it as UISegmentedControl).selectedSegmentIndex.toInt()) } }
    key(labels) {
        UIKitView(factory = { UISegmentedControl(items = labels).apply { addTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged) } },
            modifier = modifier.fillMaxWidth().height(44.dp),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
            update = { it.selectedSegmentIndex = selectedIndex.toLong() },
            onRelease = { it.removeTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged) })
    }
    return true
}

internal actual fun platformHaptic(event: HapticEvent) {
    when (event) {
        HapticEvent.Selection -> UISelectionFeedbackGenerator().selectionChanged()
        HapticEvent.Success -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        HapticEvent.Error -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
    }
}

@Composable
internal actual fun platformReduceMotion(): Boolean {
    var reduced by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(Unit) {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(UIAccessibilityReduceMotionStatusDidChangeNotification, null, NSOperationQueue.mainQueue) { reduced = UIAccessibilityIsReduceMotionEnabled() }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(token) }
    }
    return reduced
}

@Composable
internal actual fun NativeDatePicker(title: String, initialMillis: Long, maximumMillis: Long?, timeOnly: Boolean, onConfirm: (Long) -> Unit, onDismiss: () -> Unit): Boolean {
    val picker = remember { UIDatePicker().apply {
        datePickerMode = if (timeOnly) UIDatePickerMode.UIDatePickerModeTime else UIDatePickerMode.UIDatePickerModeDate
        preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
        // Shared date values represent UTC calendar days, not local instants.
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        date = NSDate.dateWithTimeIntervalSince1970(initialMillis / 1000.0)
        maximumDate = maximumMillis?.let { NSDate.dateWithTimeIntervalSince1970(it / 1000.0) }
    } }
    NativeModalSheet(onDismiss, true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss) { Text("Отмена") }
            TextButton(onClick = { val millis = (picker.date.timeIntervalSince1970 * 1000).toLong(); onConfirm(if (timeOnly) millis else (millis / 86_400_000L) * 86_400_000L); onDismiss() }) { Text("Готово") }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        UIKitView(factory = { picker }, modifier = Modifier.fillMaxWidth().height(216.dp), properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true))
    }
    return true
}
