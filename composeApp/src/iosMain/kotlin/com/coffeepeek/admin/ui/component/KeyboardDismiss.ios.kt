@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIScrollView
import platform.UIKit.UIControl
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITouch
import platform.UIKit.endEditing
import platform.darwin.NSObject
import kotlin.math.abs

@Composable
internal actual fun PlatformKeyboardDismissOnDrag() {
    val controller = LocalUIViewController.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardVisible = rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current) > 0)
    val dismiss = rememberUpdatedState {
        focusManager.clearFocus(force = true)
        // UIKit owns the keyboard and its dismissal animation, including native text inputs.
        controller.view.endEditing(true)
        keyboard?.hide()
        Unit
    }
    DisposableEffect(controller) {
        val target = KeyboardDismissGestureTarget(
            isKeyboardVisible = { keyboardVisible.value },
            onDismiss = { dismiss.value() },
        )
        controller.view.addGestureRecognizer(target.pan)
        onDispose {
            controller.view.removeGestureRecognizer(target.pan)
            target.pan.delegate = null
            target.pan.removeTarget(target, NSSelectorFromString("onPan:"))
        }
    }
}

/** Observes a native pan without cancelling the Compose or UIKit scroll gesture beneath it. */
private class KeyboardDismissGestureTarget(
    private val isKeyboardVisible: () -> Boolean,
    private val onDismiss: () -> Unit,
) : NSObject(), UIGestureRecognizerDelegateProtocol {
    val pan = UIPanGestureRecognizer(target = this, action = NSSelectorFromString("onPan:")).apply {
        maximumNumberOfTouches = 1u
        cancelsTouchesInView = false
        delaysTouchesBegan = false
        delaysTouchesEnded = false
        delegate = this@KeyboardDismissGestureTarget
    }

    @ObjCAction
    fun onPan(recognizer: UIPanGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan) onDismiss()
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        if (!isKeyboardVisible()) return false
        return pan.velocityInView(pan.view).useContents { abs(y) > abs(x) }
    }

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer,
    ): Boolean = true

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldReceiveTouch: UITouch,
    ): Boolean {
        // Preserve cursor placement, selection handles and native control interaction.
        var view = shouldReceiveTouch.view
        while (view != null) {
            if (view is UITextInputProtocol || view is UIControl || view is UIScrollView) return false
            view = view.superview
        }
        return true
    }
}
