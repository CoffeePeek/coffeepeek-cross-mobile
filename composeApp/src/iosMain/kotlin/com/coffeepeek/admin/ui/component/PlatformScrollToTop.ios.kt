@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*
import platform.darwin.NSObject

/** Only bridges UIKit's status-bar tap. LazyColumn still owns scrolling and virtualization. */
@Composable
internal actual fun PlatformScrollToTop(state: LazyListState) {
    val scope = rememberCoroutineScope()
    val reduced = platformReduceMotion()
    val action = rememberUpdatedState {
        scope.launch { if (reduced) state.scrollToItem(0) else state.animateScrollToItem(0) }
        Unit
    }
    val delegate = remember { object : NSObject(), UIScrollViewDelegateProtocol {
        override fun scrollViewShouldScrollToTop(scrollView: UIScrollView): Boolean {
            action.value()
            return false
        }
    } }
    UIKitView(factory = { UIScrollView().apply {
        backgroundColor = UIColor.clearColor
        scrollsToTop = true
        showsVerticalScrollIndicator = false
        showsHorizontalScrollIndicator = false
        setContentSize(CGSizeMake(1.0, 2.0))
        setContentOffset(CGPointMake(0.0, 1.0))
        this.delegate = delegate
        isAccessibilityElement = false
    } }, modifier = Modifier.size(1.dp),
        properties = UIKitInteropProperties(placedAsOverlay = true),
        onRelease = { it.delegate = null; it.scrollsToTop = false })
}
