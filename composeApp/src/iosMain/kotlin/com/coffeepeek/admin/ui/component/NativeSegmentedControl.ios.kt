@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlinx.cinterop.ObjCAction
import org.jetbrains.compose.ui.tooling.preview.Preview
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean): Boolean {
    if (labels.size !in 2..4) return false
    val colors = MaterialTheme.colorScheme
    val callback = rememberUpdatedState(onSelected)
    val target = remember { SegmentedControlTarget { callback.value(it) } }
    key(labels) {
        UIKitView(
            factory = { segmentedContainer(labels, target) },
            modifier = modifier.fillMaxWidth().height(44.dp),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, placedAsOverlay = false),
            update = { updateSegments(it, colors, selectedIndex, enabled) },
            onRelease = { releaseSegments(it, target) },
        )
    }
    return true
}

private class SegmentedControlTarget(private val selected: (Int) -> Unit) : NSObject() {
    @ObjCAction fun change(sender: UIControl) {
        val control = sender as UISegmentedControl
        val index = control.selectedSegmentIndex.toInt()
        if (!control.enabled || index !in 0 until control.numberOfSegments.toInt()) return
        platformHaptic(HapticEvent.Selection)
        selected(index)
    }
}

private fun segmentedContainer(labels: List<String>, target: SegmentedControlTarget): UIView {
    val control = UISegmentedControl(items = labels).apply {
        addTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged)
        translatesAutoresizingMaskIntoConstraints = false
    }
    // The rectangular underlay needs the card's color behind the pill's transparent corners.
    return UIView().apply {
        addSubview(control)
        NSLayoutConstraint.activateConstraints(listOf(
            control.topAnchor.constraintEqualToAnchor(topAnchor),
            control.bottomAnchor.constraintEqualToAnchor(bottomAnchor),
            control.leadingAnchor.constraintEqualToAnchor(leadingAnchor),
            control.trailingAnchor.constraintEqualToAnchor(trailingAnchor),
        ))
    }
}

private fun updateSegments(container: UIView, colors: ColorScheme, selectedIndex: Int, enabled: Boolean) {
    container.backgroundColor = colors.surface.toUIColor()
    container.overrideUserInterfaceStyle = colors.uiKitStyle
    val control = container.subviews.single() as UISegmentedControl
    control.enabled = enabled
    control.selectedSegmentIndex = selectedIndex.toLong()
    control.overrideUserInterfaceStyle = colors.uiKitStyle
    control.backgroundColor = colors.surface.toUIColor()
    control.selectedSegmentTintColor = colors.surface.toUIColor()
    control.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurfaceVariant.toUIColor()), UIControlStateNormal)
    control.setTitleTextAttributes(mapOf(NSForegroundColorAttributeName to colors.onSurface.toUIColor()), UIControlStateSelected)
}

private fun releaseSegments(container: UIView, target: SegmentedControlTarget) {
    val control = container.subviews.single() as UISegmentedControl
    control.enabled = false
    control.removeTarget(target, NSSelectorFromString("change:"), UIControlEventValueChanged)
}

@Composable
private fun NativeSegmentedControlPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var selected by remember { mutableIntStateOf(0) }
    NativeSegmentedControl(listOf("250 г", "1000 г"), selected, { selected = it }, Modifier.padding(16.dp))
}

@Preview @Composable private fun NativeSegmentedControlLightPreview() = NativeSegmentedControlPreview(false)
@Preview @Composable private fun NativeSegmentedControlDarkPreview() = NativeSegmentedControlPreview(true)
