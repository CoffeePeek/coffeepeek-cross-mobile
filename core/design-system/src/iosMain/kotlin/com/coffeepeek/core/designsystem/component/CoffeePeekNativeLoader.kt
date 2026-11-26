@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.accessibilityElementsHidden
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

private lateinit var nativeLoaderFactory: (UIColor, Double) -> UIViewController

fun configureCoffeePeekNativeLoader(factory: (UIColor, Double) -> UIViewController) {
    nativeLoaderFactory = factory
}

/** Native application supplies its contour renderer; no Compose child is mounted in UIKit controls. */
@Composable
fun CoffeePeekNativeLoader(parent: UIView?, color: Color, size: Dp = 24.dp) {
    if (parent == null) return
    val host = LocalUIViewController.current
    val controller = remember(size, color) { nativeLoaderFactory(color.toUIColor(), size.value.toDouble()) }
    DisposableEffect(host, controller, parent, size) {
        mountNativeLoader(host, parent, controller, size.value.toDouble())
        onDispose { unmountNativeLoader(controller) }
    }
}

private fun mountNativeLoader(host: UIViewController, parent: UIView, controller: UIViewController, size: Double) {
    host.addChildViewController(controller)
    val view = controller.view
    view.backgroundColor = UIColor.clearColor
    view.userInteractionEnabled = false
    view.accessibilityElementsHidden = true
    view.translatesAutoresizingMaskIntoConstraints = false
    parent.addSubview(view)
    NSLayoutConstraint.activateConstraints(listOf(
        view.centerXAnchor.constraintEqualToAnchor(parent.centerXAnchor),
        view.centerYAnchor.constraintEqualToAnchor(parent.centerYAnchor),
        view.widthAnchor.constraintEqualToConstant(size),
        view.heightAnchor.constraintEqualToConstant(size),
    ))
    controller.didMoveToParentViewController(host)
}

private fun unmountNativeLoader(controller: UIViewController) {
    if (controller.parentViewController == null) return
    controller.willMoveToParentViewController(null)
    controller.view.removeFromSuperview()
    controller.removeFromParentViewController()
}

private val coffeePeekLoaderImage by lazy {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { opaque = false }
    UIGraphicsImageRenderer(size = CGSizeMake(24.0, 24.0), format = format).imageWithActions { }
}

/** Reserves the native button's image slot without enabling its system spinner. */
fun coffeePeekLoaderPlaceholderImage(): UIImage = coffeePeekLoaderImage
