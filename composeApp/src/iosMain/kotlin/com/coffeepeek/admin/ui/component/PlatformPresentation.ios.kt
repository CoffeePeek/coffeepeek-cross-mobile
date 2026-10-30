@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.window.ComposeUIViewController
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.presentation_cancel
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.theme.switchPalette
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

internal fun UIViewController.presentationHost(): UIViewController {
    var host = this
    while (host.presentedViewController != null && !host.presentedViewController!!.isBeingDismissed()) {
        host = host.presentedViewController!!
    }
    return host
}

/** Presentation can be requested by state before its screen's push animation has finished. */
@Composable
internal fun PresentNativeController(controller: UIViewController, host: UIViewController) {
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
    IosActionDialog(title, message, listOf(PlatformAction(confirmLabel, destructive, enabled, onConfirm)),
        cancelLabel, dismissBeforeAction = false, onDismiss = onDismiss)
}

@Composable
internal actual fun NativeAlert(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean): Boolean {
    PlatformAlert(title, message, confirmLabel, onConfirm, onDismiss, cancelLabel, destructive)
    return true
}

@Composable
internal actual fun NativeActionSheet(title: String, actions: List<PlatformAction>, message: String?, dismissBeforeAction: Boolean, onDismiss: () -> Unit): Boolean {
    IosActionDialog(title, message, actions, stringResource(Res.string.presentation_cancel),
        dismissBeforeAction, onDismiss, UIAlertControllerStyleActionSheet)
    return true
}

@Composable
private fun IosActionDialog(
    title: String, message: String?, actions: List<PlatformAction>, cancelLabel: String?,
    dismissBeforeAction: Boolean, onDismiss: () -> Unit,
    style: UIAlertControllerStyle = UIAlertControllerStyleAlert,
) {
    val host = LocalUIViewController.current
    val dismiss = rememberUpdatedState(onDismiss)
    val latestActions = rememberUpdatedState(actions)
    val colors = MaterialTheme.colorScheme
    val themeMode by ThemeManager.themeMode.collectAsState()
    val actionLabels = actions.map { it.title to it.destructive }
    val alert = remember(host, title, message, actionLabels, cancelLabel, style, dismissBeforeAction) {
        val selection = PlatformActionSelection(dismissBeforeAction) { dismiss.value() }
        UIAlertController.alertControllerWithTitle(title, message, style).apply {
            addNativeActions(this, actions, { latestActions.value }, selection)
            if (cancelLabel != null) addAction(UIAlertAction.actionWithTitle(cancelLabel, UIAlertActionStyleCancel) {
                dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled()) { dismiss.value() }
            })
            popoverPresentationController?.apply {
                sourceView = host.view
                sourceRect = host.view.bounds
                permittedArrowDirections = 0u
            }
        }
    }
    SideEffect {
        alert.overrideUserInterfaceStyle = themeMode.uiKitStyle
        alert.view.tintColor = colors.primary.toUIColor()
        actions.forEachIndexed { index, action -> (alert.actions[index] as UIAlertAction).enabled = action.enabled }
    }
    PresentNativeController(alert, host)
}

private fun addNativeActions(
    alert: UIAlertController, actions: List<PlatformAction>, latestActions: () -> List<PlatformAction>,
    selection: PlatformActionSelection,
) {
    actions.forEachIndexed { index, action ->
        val style = if (action.destructive) UIAlertActionStyleDestructive else UIAlertActionStyleDefault
        alert.addAction(UIAlertAction.actionWithTitle(action.title, style) {
            val currentAction = latestActions().getOrNull(index) ?: return@actionWithTitle
            selection.select(currentAction) { completed ->
                alert.dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled(), completed)
            }
        }.apply { enabled = action.enabled })
    }
}

@Composable
internal actual fun NativeModalSheet(
    onDismiss: () -> Unit, dismissEnabled: Boolean, content: @Composable ColumnScope.() -> Unit,
): Boolean = com.coffeepeek.core.designsystem.component.CoffeePeekNativeModalSheet(
    onDismiss, dismissEnabled, LocalNativeSheetHeightFraction.current,
    Modifier.dismissKeyboardOnBackgroundGestures(), content,
)

private class ControlTarget(var changed: (UIControl) -> Unit) : NSObject() {
    @ObjCAction fun change(sender: UIControl) = changed(sender)
}

@Composable
internal actual fun PlatformSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean, modifier: Modifier) {
    com.coffeepeek.core.designsystem.component.CoffeePeekSwitch(checked, onCheckedChange?.let { callback -> { value -> platformHaptic(HapticEvent.Selection); callback(value) } }, modifier, enabled)
}

@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier): Boolean {
    if (labels.size !in 2..4) return false
    val colors = MaterialTheme.colorScheme
    val callback = rememberUpdatedState(onSelected)
    val target = remember { ControlTarget { platformHaptic(HapticEvent.Selection); callback.value((it as UISegmentedControl).selectedSegmentIndex.toInt()) } }
    key(labels) {
        UIKitView(factory = {
            val control = UISegmentedControl(items = labels).apply {
                addTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged)
                translatesAutoresizingMaskIntoConstraints = false
            }
            // The underlay hole is rectangular; its transparent pill corners need the card's color.
            UIView().apply {
                addSubview(control)
                NSLayoutConstraint.activateConstraints(listOf(
                    control.topAnchor.constraintEqualToAnchor(topAnchor),
                    control.bottomAnchor.constraintEqualToAnchor(bottomAnchor),
                    control.leadingAnchor.constraintEqualToAnchor(leadingAnchor),
                    control.trailingAnchor.constraintEqualToAnchor(trailingAnchor),
                ))
            }
        },
            modifier = modifier.fillMaxWidth().height(44.dp),
            // Embedded segments must respect Compose clipping and draw order when scrolled.
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = false),
            update = { container ->
                container.backgroundColor = colors.surface.toUIColor()
                container.overrideUserInterfaceStyle = colors.uiKitStyle
                val control = container.subviews.single() as UISegmentedControl
                control.selectedSegmentIndex = selectedIndex.toLong()
                control.overrideUserInterfaceStyle = colors.uiKitStyle
                control.backgroundColor = colors.surface.toUIColor()
                control.selectedSegmentTintColor = colors.surface.toUIColor()
                control.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurfaceVariant.toUIColor()), UIControlStateNormal)
                control.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurface.toUIColor()), UIControlStateSelected)
            },
            onRelease = { container ->
                (container.subviews.single() as UISegmentedControl)
                    .removeTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged)
            })
    }
    return true
}

@Composable
private fun NativeSegmentedControlPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var selected by remember { mutableIntStateOf(0) }
    NativeSegmentedControl(listOf("250 г", "1000 г"), selected, { selected = it }, Modifier.padding(16.dp))
}

@Preview @Composable private fun NativeSegmentedControlLightPreview() = NativeSegmentedControlPreview(false)
@Preview @Composable private fun NativeSegmentedControlDarkPreview() = NativeSegmentedControlPreview(true)

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
    // Use the shared entry point so iOS 15 can also show the wheel in a fitted
    // sheet. NativeTimePickerRow relies on this function handling presentation.
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
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

@Composable
private fun NativeSwitchPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PlatformSwitch(true, {})
}

@Preview @Composable private fun NativeSwitchLightPreview() = NativeSwitchPreview(false)
@Preview @Composable private fun NativeSwitchDarkPreview() = NativeSwitchPreview(true)
