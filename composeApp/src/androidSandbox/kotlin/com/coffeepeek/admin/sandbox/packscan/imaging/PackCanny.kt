package com.coffeepeek.admin.sandbox.packscan.imaging

import kotlin.math.abs

/**
 * Canny edges of a 0..255 plane: 3x3 Sobel with L1 magnitude, non-maximum suppression in four directions
 * and hysteresis between [low] and [high] (OpenCV's defaults for cv2.Canny).
 */
internal fun Plane.canny(low: Float, high: Float): BooleanArray {
    val (gx, gy) = sobel()
    val magnitude = FloatArray(data.size) { abs(gx.data[it]) + abs(gy.data[it]) }
    val strong = BooleanArray(data.size)
    val weak = BooleanArray(data.size)
    for (y in 1 until height - 1) for (x in 1 until width - 1) {
        val i = y * width + x
        val m = magnitude[i]
        if (m <= low || !isLocalMaximum(magnitude, gx.data[i], gy.data[i], i)) continue
        if (m > high) strong[i] = true else weak[i] = true
    }
    return hysteresis(strong, weak)
}

// Compares the magnitude with its two neighbours across the edge (gradient direction, y grows downwards).
private fun Plane.isLocalMaximum(magnitude: FloatArray, gx: Float, gy: Float, i: Int): Boolean {
    val ax = abs(gx)
    val ay = abs(gy)
    val step = when {
        ay <= ax * TAN_22_5 -> 1
        ay > ax * TAN_67_5 -> width
        gx * gy > 0 -> width + 1
        else -> width - 1
    }
    return magnitude[i] > magnitude[i - step] && magnitude[i] >= magnitude[i + step]
}

// Keeps weak edges 8-connected to a strong one.
private fun Plane.hysteresis(strong: BooleanArray, weak: BooleanArray): BooleanArray {
    val edges = strong.copyOf()
    val stack = ArrayDeque<Int>()
    strong.indices.filterTo(stack) { strong[it] }
    while (stack.isNotEmpty()) {
        val i = stack.removeLast()
        for (dy in -1..1) for (dx in -1..1) {
            val j = i + dy * width + dx
            if (j in weak.indices && weak[j] && !edges[j]) {
                edges[j] = true
                stack.addLast(j)
            }
        }
    }
    return edges
}

private const val TAN_22_5 = 0.41421357f
private const val TAN_67_5 = 2.4142137f
