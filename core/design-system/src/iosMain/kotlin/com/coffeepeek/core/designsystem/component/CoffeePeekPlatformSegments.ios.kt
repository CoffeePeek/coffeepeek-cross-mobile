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
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarMetricsDefault
import platform.UIKit.UIColor
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIImage
import platform.UIKit.UISegmentedControl
import platform.UIKit.UIView
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
        factory = { CoffeePeekGlassSegments(labels, target) },
        modifier = modifier.fillMaxWidth().height(48.dp),
        update = { view ->
            val control = view.control
            if (control.numberOfSegments.toInt() != labels.size) {
                control.removeAllSegments()
                labels.forEachIndexed { index, label -> control.insertSegmentWithTitle(label, index.toULong(), false) }
            } else labels.forEachIndexed { index, label -> control.setTitle(label, index.toULong()) }
            control.selectedSegmentIndex = selectedIndex.toLong()
            control.enabled = enabled
            control.tintColor = colors.primary.toUIColor()
            view.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        },
        onRelease = { it.control.removeTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged) },
    )
}

/** Remove UIKit's solid track; one native glass material owns the surrounding capsule. */
private class CoffeePeekGlassSegments(labels: List<String>, target: CoffeePeekSegmentTarget) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val control = UISegmentedControl(items = labels)
    private val glass = coffeePeekGlassMaterial()

    init {
        backgroundColor = UIColor.clearColor
        addSubview(glass)
        glass.contentView.addSubview(control)
        control.backgroundColor = UIColor.clearColor
        control.setBackgroundImage(UIImage(), UIControlStateNormal, UIBarMetricsDefault)
        control.addTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        glass.setFrame(bounds)
        glass.updateCoffeePeekGlassCorners()
        control.setFrame(glass.contentView.bounds)
    }
}

private class CoffeePeekSegmentTarget(val callback: (Int) -> Unit) : NSObject() {
    @ObjCAction fun changed(sender: UISegmentedControl) = callback(sender.selectedSegmentIndex.toInt())
}
