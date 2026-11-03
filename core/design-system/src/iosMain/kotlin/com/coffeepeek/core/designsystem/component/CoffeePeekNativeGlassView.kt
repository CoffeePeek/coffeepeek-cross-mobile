@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.rememberUIKitInteropRemeasureRequester
import androidx.compose.ui.viewinterop.remeasureRequester
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.QuartzCore.CAShapeLayer
import platform.UIKit.UIBezierPath
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/** Native glass samples Compose below it; the visible viewport still clips scrolling controls. */
@Composable
@Suppress("UNCHECKED_CAST")
fun <T : UIView> CoffeePeekNativeGlassView(
    factory: () -> T,
    modifier: Modifier = Modifier,
    interactionMode: UIKitInteropInteractionMode? = UIKitInteropInteractionMode.Cooperative(),
    accessibilityEnabled: Boolean = true,
    onRelease: (T) -> Unit = {},
    update: (T) -> Unit,
) {
    val density = LocalDensity.current.density
    val clipping = remember(density) { CoffeePeekNativeOverlayClipping(density) }
    val measurement = rememberUIKitInteropRemeasureRequester()
    UIKitView(
        // Configure text/images before UIKit's first intrinsic measurement.
        factory = { CoffeePeekNativeOverlayContainer(factory()).also {
            clipping.view = it
            update(it.content as T)
            it.invalidateIntrinsicContentSize()
        } },
        modifier = modifier.remeasureRequester(measurement).onGloballyPositioned(clipping::position),
        properties = UIKitInteropProperties(interactionMode = interactionMode,
            isNativeAccessibilityEnabled = accessibilityEnabled, placedAsOverlay = true),
        update = {
            clipping.view = it
            // UIKit invalidation alone does not invalidate Compose's measurement.
            // Refit after asynchronous icons, text, selection or Dynamic Type change.
            update(it.content as T)
            it.invalidateIntrinsicContentSize()
            measurement.requestRemeasure()
            clipping.apply()
        },
        onRelease = {
            clipping.view = null
            onRelease(it.content as T)
        },
    )
}

private class CoffeePeekNativeOverlayClipping(private val density: Float) {
    var view: CoffeePeekNativeOverlayContainer? = null
    private var clip: Rect? = null

    fun position(coordinates: LayoutCoordinates) {
        clip = coffeePeekNativeOverlayBounds(coordinates.boundsInRoot(), coordinates.positionInRoot(),
            Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
        apply()
    }

    fun apply() {
        val view = view ?: return
        val clip = clip
        view.visibleClip = clip?.let { Rect(it.left / density, it.top / density, it.right / density, it.bottom / density) }
        view.hidden = clip?.isEmpty == true
        view.layer.mask = clip?.takeUnless { it.isEmpty }?.let {
            CAShapeLayer().apply {
                path = UIBezierPath.bezierPathWithRect(CGRectMake(
                    (it.left / density).toDouble(), (it.top / density).toDouble(),
                    (it.width / density).toDouble(), (it.height / density).toDouble(),
                )).CGPath
            }
        }
    }
}

/** A layer mask changes pixels, not UIKit hit testing. Both use the same viewport. */
private class CoffeePeekNativeOverlayContainer(val content: UIView) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var visibleClip: Rect? = null

    init { addSubview(content) }

    override fun layoutSubviews() {
        super.layoutSubviews()
        content.setFrame(bounds)
    }

    override fun intrinsicContentSize(): CValue<CGSize> = content.intrinsicContentSize()

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = content.sizeThatFits(size)

    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean {
        if (!super.pointInside(point, withEvent)) return false
        val clip = visibleClip ?: return true
        return point.useContents { x >= clip.left && x < clip.right && y >= clip.top && y < clip.bottom }
    }
}
