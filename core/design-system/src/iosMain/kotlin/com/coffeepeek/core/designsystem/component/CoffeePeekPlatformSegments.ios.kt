@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISegmentedControl
import platform.darwin.NSObject

@Composable
internal actual fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
) {
    val callback = rememberUpdatedState(onSelected)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    val target = remember { CoffeePeekSegmentTarget { if (active.value) callback.value(it) } }
    CoffeePeekNativeGlassView(
        factory = { UISegmentedControl(items = labels).apply {
            addTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
        } },
        modifier = modifier.fillMaxWidth().height(48.dp),
        update = { control ->
            if (control.numberOfSegments.toInt() != labels.size) {
                control.removeAllSegments()
                labels.forEachIndexed { index, label -> control.insertSegmentWithTitle(label, index.toULong(), false) }
            } else labels.forEachIndexed { index, label -> control.setTitle(label, index.toULong()) }
            control.selectedSegmentIndex = selectedIndex.toLong()
            control.enabled = enabled
            control.tintColor = colors.primary.toUIColor()
            control.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        },
        onRelease = { it.removeTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged) },
    )
}

private class CoffeePeekSegmentTarget(val callback: (Int) -> Unit) : NSObject() {
    @ObjCAction fun changed(sender: UISegmentedControl) = callback(sender.selectedSegmentIndex.toInt())
}
