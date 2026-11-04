package com.coffeepeek.feature.discovery.impl.tab

import kotlin.math.roundToInt

/** A rectangle in pixels; the maths of the tab needs no more than this, so it stays free of the UI toolkit and testable. */
internal data class Frame(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val Zero = Frame(0f, 0f, 0f, 0f)
        fun of(left: Float, top: Float, width: Float, height: Float) = Frame(left, top, left + width, top + height)
    }
}

private fun lerp(from: Float, to: Float, fraction: Float) = from + (to - from) * fraction

/**
 * Lazy item offsets exclude [beforeContentPadding]; include it to preserve the gap below the header.
 * Follow scrolling without pull-to-refresh displacement, and park recycled items above the viewport.
 */
internal fun previewFrame(anchor: Frame, viewportTop: Float, itemOffset: Int?, beforeContentPadding: Int): Frame {
    val top = if (itemOffset == null) viewportTop - anchor.height
        else viewportTop + beforeContentPadding + itemOffset
    return Frame.of(anchor.left, top, anchor.width, anchor.height)
}

/** The map between its small place ([expansion] 0) and the whole screen (1). */
internal fun expandedFrame(preview: Frame, screenWidth: Float, screenHeight: Float, expansion: Float): Frame {
    val t = expansion.coerceIn(0f, 1f)
    return Frame(lerp(preview.left, 0f, t), lerp(preview.top, 0f, t), lerp(preview.right, screenWidth, t), lerp(preview.bottom, screenHeight, t))
}

/** The map slides away upward as the shop list takes over ([progress] 1 = discovery shown): the list never overlaps the map. */
internal fun discoveryMapOffsetY(viewportHeight: Float, progress: Float): Float = viewportHeight * (progress.coerceIn(0f, 1f) - 1f)

/** The map stays below the separator while collapsed and may use the whole screen when expanded: the top edge it is clipped at. */
internal fun mapClipTop(viewportTop: Float, expansion: Float, screenHeight: Int): Int =
    (viewportTop * (1f - expansion)).roundToInt().coerceIn(0, screenHeight)

/** The corner radius of the map shrinks to a square screen as it expands. */
internal fun mapCornerRadius(radius: Float, expansion: Float): Float = radius * (1f - expansion.coerceIn(0f, 1f))
