@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceTransparencyEnabled
import platform.UIKit.UIAccessibilityReduceTransparencyStatusDidChangeNotification

/** Compose-only frost must observe the preference that native UIKit materials handle themselves. */
@Composable
internal actual fun coffeePeekFrostedSurfacePlatformOpaque(): Boolean {
    var opaque by remember { mutableStateOf(UIAccessibilityIsReduceTransparencyEnabled()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(UIAccessibilityReduceTransparencyStatusDidChangeNotification,
            null, NSOperationQueue.mainQueue) { opaque = UIAccessibilityIsReduceTransparencyEnabled() }
        onDispose { center.removeObserver(observer) }
    }
    return opaque
}
