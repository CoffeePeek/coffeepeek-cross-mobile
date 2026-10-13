@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

/** UIKit owns scrolling and keyboard interaction; Compose measures the form without a viewport limit. */
private class FormScrollController : UIViewController(nibName = null, bundle = null) {
    val scroll = UIScrollView().apply {
        alwaysBounceVertical = true
        keyboardDismissMode = UIScrollViewKeyboardDismissMode.UIScrollViewKeyboardDismissModeInteractive
        scrollsToTop = true
    }
    var body: UIViewController? = null
    var contentHeight = 1.0
    override fun loadView() { view = scroll }
    override fun viewDidLoad() {
        super.viewDidLoad()
        body?.let { addChildViewController(it); scroll.addSubview(it.view); it.didMoveToParentViewController(this) }
    }
    override fun viewDidLayoutSubviews() { super.viewDidLayoutSubviews(); resize() }
    fun resize() {
        val width = scroll.bounds.useContents { size.width }
        body?.view?.setFrame(CGRectMake(0.0, 0.0, width, contentHeight))
        scroll.setContentSize(CGSizeMake(width, contentHeight))
    }
}

@Composable
internal actual fun NativeScrollColumn(modifier: Modifier, arrangement: Arrangement.Vertical, alignment: Alignment.Horizontal, content: @Composable ColumnScope.() -> Unit): Boolean {
    val body = rememberUpdatedState(content)
    val currentArrangement = rememberUpdatedState(arrangement)
    val currentAlignment = rememberUpdatedState(alignment)
    val controller = remember { FormScrollController() }
    UIKitViewController(factory = {
        controller.body = ComposeUIViewController {
            AppContent {
                val density = LocalDensity.current.density
                Layout(content = { Column(Modifier.fillMaxWidth(), currentArrangement.value, currentAlignment.value, body.value) }) { measurables, constraints ->
                    val item = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                    val height = item.height / density.toDouble()
                    if (controller.contentHeight != height) {
                        controller.contentHeight = height
                        // UIKit frame updates must happen outside Compose's measurement pass.
                        NSOperationQueue.mainQueue.addOperationWithBlock { controller.resize() }
                    }
                    layout(constraints.maxWidth, constraints.constrainHeight(item.height)) { item.place(0, 0) }
                }
            }
        }
        controller
    }, modifier = modifier.fillMaxSize(), properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative))
    DisposableEffect(controller) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(UIKeyboardWillChangeFrameNotification, null, NSOperationQueue.mainQueue) { note ->
            val frame = (note?.userInfo?.get(UIKeyboardFrameEndUserInfoKey) as? NSValue)?.CGRectValue ?: return@addObserverForName
            val rect = controller.scroll.convertRect(frame, fromView = null)
            val bottom = controller.scroll.bounds.useContents { origin.y + size.height }
            val overlap = rect.useContents { (bottom - origin.y).coerceIn(0.0, size.height) }
            controller.scroll.contentInset = UIEdgeInsetsMake(0.0, 0.0, overlap, 0.0)
            controller.scroll.scrollIndicatorInsets = controller.scroll.contentInset
            // The native responder gets scrolled into view after the keyboard is shown.
            if (overlap > 0) controller.scroll.findResponder()?.let { responder ->
                controller.scroll.scrollRectToVisible(responder.convertRect(responder.bounds, toView = controller.scroll), true)
            }
        }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }
    return true
}

private fun UIView.findResponder(): UIView? = if (isFirstResponder()) this else subviews.filterIsInstance<UIView>().firstNotNullOfOrNull { it.findResponder() }
