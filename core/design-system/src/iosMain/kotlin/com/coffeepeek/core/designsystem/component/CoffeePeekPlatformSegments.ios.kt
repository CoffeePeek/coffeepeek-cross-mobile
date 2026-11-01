@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarMetricsDefault
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIColor
import platform.UIKit.UIControlStateDisabled
import platform.UIKit.UIControlStateHighlighted
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIControlStateSelected
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UISegmentedControl
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.animateWithDuration
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSObject
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal actual fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
) {
    val callback = rememberUpdatedState(onSelected)
    val active = rememberUpdatedState(enabled)
    val colors = MaterialTheme.colorScheme
    CoffeePeekNativeGlassView(
        factory = { CoffeePeekGlassSegments(labels) { if (active.value) callback.value(it) } },
        modifier = modifier.fillMaxWidth().height(48.dp),
        update = { view ->
            val control = view.control
            if (control.numberOfSegments.toInt() != labels.size) {
                control.removeAllSegments()
                labels.forEachIndexed { index, label -> control.insertSegmentWithTitle(label, index.toULong(), false) }
            } else labels.forEachIndexed { index, label -> control.setTitle(label, index.toULong()) }
            view.updateSelection(selectedIndex, enabled, colors.onSurface, colors.background.luminance() < .5f)
            control.tintColor = colors.primary.toUIColor()
            view.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        },
        onRelease = { it.release() },
    )
}

/** UIKit still owns interaction and accessibility; its segment masks no longer crop the visible pill. */
private fun UISegmentedControl.clearCoffeePeekSegmentBackgrounds() {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { opaque = false }
    val image = UIGraphicsImageRenderer(CGSizeMake(1.0, 1.0), format).imageWithActions {}
    val states = listOf(UIControlStateNormal, UIControlStateSelected).flatMap { state ->
        listOf(state, state or UIControlStateHighlighted, state or UIControlStateDisabled,
            state or UIControlStateHighlighted or UIControlStateDisabled)
    }
    states.forEach { state ->
        setBackgroundImage(image, state, UIBarMetricsDefault)
    }
    states.forEach { left ->
        states.forEach { right -> setDividerImage(image, left, right, UIBarMetricsDefault) }
    }
    selectedSegmentTintColor = UIColor.clearColor
}

/** Remove UIKit's solid track; one native glass material owns the surrounding capsule. */
private class CoffeePeekGlassSegments(labels: List<String>, onSelected: (Int) -> Unit) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val control = UISegmentedControl(items = labels)
    private val glass = coffeePeekGlassMaterial()
    private val selection = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    private var selectionFrame: CoffeePeekSegmentSelectionFrame? = null
    private val target = CoffeePeekSegmentTarget { index ->
        updateSelectionFrame()
        onSelected(index)
    }

    init {
        backgroundColor = UIColor.clearColor
        addSubview(glass)
        glass.contentView.addSubview(selection)
        glass.contentView.addSubview(control)
        selection.userInteractionEnabled = false
        selection.isAccessibilityElement = false
        selection.layer.borderWidth = .75
        control.backgroundColor = UIColor.clearColor
        control.apportionsSegmentWidthsByContent = false
        control.clearCoffeePeekSegmentBackgrounds()
        control.addTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
    }

    fun updateSelection(index: Int, enabled: Boolean, foreground: Color, dark: Boolean) {
        control.selectedSegmentIndex = index.toLong()
        control.enabled = enabled
        selection.backgroundColor = foreground.copy(alpha = if (dark) .22f else .12f).toUIColor()
        selection.layer.borderColor = foreground.copy(alpha = if (dark) .28f else .22f).toUIColor().CGColor
        selection.alpha = if (enabled) 1.0 else .45
        updateSelectionFrame()
    }

    fun release() {
        control.removeTarget(target, NSSelectorFromString("changed:"), UIControlEventValueChanged)
        target.callback = null
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        glass.setFrame(bounds)
        glass.updateCoffeePeekGlassCorners()
        control.setFrame(glass.contentView.bounds)
        updateSelectionFrame()
    }

    private fun updateSelectionFrame() {
        val count = control.numberOfSegments.toInt()
        val index = control.selectedSegmentIndex.toInt()
        selection.hidden = index !in 0 until count
        if (selection.hidden) {
            selectionFrame = null
            return
        }
        val width = glass.contentView.bounds.useContents { size.width } / count
        val height = (glass.contentView.bounds.useContents { size.height } - 6.0).coerceAtLeast(0.0)
        val rtl = control.effectiveUserInterfaceLayoutDirection == UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft
        val position = if (rtl) count - 1 - index else index
        moveSelection(CoffeePeekSegmentSelectionFrame(position * width + 3.0, (width - 6.0).coerceAtLeast(0.0), height))
    }

    private fun moveSelection(frame: CoffeePeekSegmentSelectionFrame) {
        val previous = selectionFrame
        if (frame == previous) return
        selectionFrame = frame
        val animate = previous != null && previous.width == frame.width && previous.height == frame.height && !UIAccessibilityIsReduceMotionEnabled()
        if (animate) UIView.animateWithDuration(.2, animations = { frame.place(selection) })
        else frame.place(selection)
    }
}

private data class CoffeePeekSegmentSelectionFrame(val x: Double, val width: Double, val height: Double) {
    fun place(view: UIView) {
        view.setFrame(CGRectMake(x, 3.0, width, height))
        view.layer.cornerRadius = height / 2.0
    }
}

private class CoffeePeekSegmentTarget(var callback: ((Int) -> Unit)?) : NSObject() {
    @ObjCAction fun changed(sender: UISegmentedControl) {
        callback?.invoke(sender.selectedSegmentIndex.toInt())
    }
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
