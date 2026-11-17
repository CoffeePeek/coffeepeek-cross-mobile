@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIResponder
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

/** Dialogs may override LocalUIViewController for camera presentation; containment follows the mounted view. */
@Composable
internal fun NativeEditorControllerHost(controller: UIViewController, modifier: Modifier) {
    key(controller) {
        UIKitView(
            factory = { NativeEditorContainer(controller) },
            modifier = modifier,
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
            onRelease = { it.release() },
        )
    }
}

private class NativeEditorContainer(private val controller: UIViewController) :
    UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var released = false

    override fun didMoveToSuperview() {
        super.didMoveToSuperview()
        attachToMountedParent()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        attachToMountedParent()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        attachToMountedParent()
        if (controller.view.superview == this) controller.view.setFrame(bounds)
    }

    private fun attachToMountedParent() {
        if (released) return
        val parent = mountedParent() ?: return
        if (controller.parentViewController == parent) return
        detach()
        parent.addChildViewController(controller)
        controller.view.setFrame(bounds)
        addSubview(controller.view)
        controller.didMoveToParentViewController(parent)
    }

    private fun mountedParent(): UIViewController? {
        var responder: UIResponder? = nextResponder
        while (responder != null) {
            if (responder is UIViewController) return responder
            responder = responder.nextResponder
        }
        return null
    }

    fun release() {
        released = true
        detach()
    }

    private fun detach() {
        if (controller.parentViewController == null) return
        controller.willMoveToParentViewController(null)
        controller.view.removeFromSuperview()
        controller.removeFromParentViewController()
    }
}
