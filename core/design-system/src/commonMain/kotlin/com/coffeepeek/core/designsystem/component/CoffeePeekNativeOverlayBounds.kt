package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Converts the ancestor-clipped viewport into a native control's local coordinates. */
internal fun coffeePeekNativeOverlayBounds(visible: Rect, origin: Offset, size: Size): Rect? {
    val full = Rect(Offset.Zero, size)
    return visible.translate(-origin).intersect(full).takeUnless { it == full }
}
