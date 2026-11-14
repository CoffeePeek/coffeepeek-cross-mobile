@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISegmentedControl
import platform.darwin.NSObject
import org.jetbrains.compose.ui.tooling.preview.Preview

/** UIKit owns the selection material, corners and native interaction feedback. */
@Composable
internal actual fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
    minSegmentHeight: Dp,
) {
    val callback = rememberUpdatedState(onSelected)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    val target = remember { CoffeePeekSegmentTarget { if (active.value) callback.value(it) } }
    CoffeePeekNativeGlassView(
        factory = { UISegmentedControl(items = labels).apply {
            addTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
        } },
        modifier = modifier.fillMaxWidth().heightIn(min = maxOf(minSegmentHeight, CoffeePeekDimens.minimumTouchTarget)),
        update = { control ->
            control.updateCoffeePeekLabels(labels)
            control.selectedSegmentIndex = selectedIndex.toLong()
            control.enabled = enabled
            control.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        },
        onRelease = { it.removeTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged) },
    )
}

private fun UISegmentedControl.updateCoffeePeekLabels(labels: List<String>) {
    if (numberOfSegments.toInt() != labels.size) {
        removeAllSegments()
        labels.forEachIndexed { index, label -> insertSegmentWithTitle(label, index.toULong(), false) }
    } else labels.forEachIndexed { index, label -> setTitle(label, index.toULong()) }
}

private class CoffeePeekSegmentTarget(val callback: (Int) -> Unit) : NSObject() {
    @ObjCAction fun changed(sender: UISegmentedControl) = callback(sender.selectedSegmentIndex.toInt())
}

@Composable
private fun CoffeePeekNativeSegmentsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (2..4).forEach { count ->
            val labels = List(count) { ('A' + it).toString() }
            var selected by remember { mutableIntStateOf(0) }
            CoffeePeekPlatformSegments(labels, selected, { selected = it }, Modifier, enabled = true)
            CoffeePeekPlatformSegments(labels, count - 1, {}, Modifier, enabled = true)
        }
        CoffeePeekPlatformSegments(listOf("A", "B"), 0, {}, Modifier, enabled = false)
    }
}

@Preview @Composable private fun CoffeePeekNativeSegmentsLightPreview() = CoffeePeekNativeSegmentsPreview(false)
@Preview @Composable private fun CoffeePeekNativeSegmentsDarkPreview() = CoffeePeekNativeSegmentsPreview(true)
