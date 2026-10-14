@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.theme.switchPalette
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.theme.ThemeManager
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
    val colors = MaterialTheme.colorScheme
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
    SideEffect {
        alert.overrideUserInterfaceStyle = colors.uiKitStyle
        alert.view.tintColor = colors.primary.toUIColor()
        (alert.actions.firstOrNull() as? UIAlertAction)?.enabled = enabled
    }
}

@Composable
internal actual fun NativeActionSheet(title: String, actions: List<PlatformAction>, onDismiss: () -> Unit): Boolean {
    val host = LocalUIViewController.current
    val colors = MaterialTheme.colorScheme
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
    SideEffect {
        alert.overrideUserInterfaceStyle = colors.uiKitStyle
        alert.view.tintColor = colors.primary.toUIColor()
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
    val colors = MaterialTheme.colorScheme
    val themeMode by ThemeManager.themeMode.collectAsState()
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
        controller.overrideUserInterfaceStyle = themeMode.uiKitStyle
        controller.view.backgroundColor = colors.surface.toUIColor()
        controller.view.tintColor = colors.primary.toUIColor()
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
    val palette = switchPalette(MaterialTheme.colorScheme.background.luminance() < .5f)
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { callback ->
            { value -> platformHaptic(HapticEvent.Selection); callback(value) }
        },
        enabled = enabled,
        modifier = modifier,
        thumbContent = { Box(Modifier.size(SwitchDefaults.IconSize)) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = palette.checkedThumb,
            checkedTrackColor = palette.checkedTrack,
            uncheckedThumbColor = palette.uncheckedThumb,
            uncheckedTrackColor = palette.uncheckedTrack,
            checkedBorderColor = Color.Transparent,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = palette.checkedThumb,
            disabledCheckedTrackColor = palette.checkedTrack.copy(alpha = .5f),
            disabledUncheckedThumbColor = palette.uncheckedThumb,
            disabledUncheckedTrackColor = palette.uncheckedTrack.copy(alpha = .5f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier): Boolean {
    if (labels.size !in 2..4) return false
    val colors = MaterialTheme.colorScheme
    val callback = rememberUpdatedState(onSelected)
    val target = remember { ControlTarget { platformHaptic(HapticEvent.Selection); callback.value((it as UISegmentedControl).selectedSegmentIndex.toInt()) } }
    key(labels) {
        UIKitView(factory = { UISegmentedControl(items = labels).apply { addTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged) } },
            modifier = modifier.fillMaxWidth().height(44.dp),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
            update = {
                it.selectedSegmentIndex = selectedIndex.toLong()
                it.overrideUserInterfaceStyle = colors.uiKitStyle
                it.backgroundColor = UIColor.clearColor
                it.selectedSegmentTintColor = colors.surface.toUIColor()
                it.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurfaceVariant.toUIColor()), UIControlStateNormal)
                it.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurface.toUIColor()), UIControlStateSelected)
            },
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
        val colors = MaterialTheme.colorScheme
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss) { Text("Отмена") }
            TextButton(onClick = { val millis = (picker.date.timeIntervalSince1970 * 1000).toLong(); onConfirm(if (timeOnly) millis else (millis / 86_400_000L) * 86_400_000L); onDismiss() }) { Text("Готово") }
        }
        PlatformScrollableColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            UIKitView(
                factory = { picker },
                modifier = Modifier.fillMaxWidth().height(216.dp),
                properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = true),
                update = {
                    it.overrideUserInterfaceStyle = colors.uiKitStyle
                    it.backgroundColor = UIColor.clearColor
                    it.tintColor = colors.primary.toUIColor()
                },
            )
        }
    }
    return true
}
