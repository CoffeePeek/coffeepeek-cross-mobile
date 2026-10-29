package com.coffeepeek.feature.shelf.data.scan.geometry

/** Square dilation by [radius], as two separable passes. */
internal fun dilate(mask: BooleanArray, width: Int, radius: Int): BooleanArray {
    val height = mask.size / width
    val rows = BooleanArray(mask.size)
    for (y in 0 until height) spread(mask, rows, start = y * width, step = 1, count = width, radius = radius)
    val out = BooleanArray(mask.size)
    for (x in 0 until width) spread(rows, out, start = x, step = width, count = height, radius = radius)
    return out
}

// Marks every cell of one line that lies within `radius` of a set source cell.
private fun spread(source: BooleanArray, target: BooleanArray, start: Int, step: Int, count: Int, radius: Int) {
    var last = -radius - 1
    for (i in 0 until count) {
        if (source[start + i * step]) last = i
        if (i - last <= radius) target[start + i * step] = true
    }
    last = count + radius
    for (i in count - 1 downTo 0) {
        if (source[start + i * step]) last = i
        if (last - i <= radius) target[start + i * step] = true
    }
}

/**
 * Replaces pixels outside the bag with the nearest bag pixel of their row, and empty rows with the
 * nearest filled row, so the inpainting model sees bag-like context instead of transparent black.
 */
internal fun extendOutward(pixels: IntArray, inside: BooleanArray, width: Int) {
    val height = pixels.size / width
    val filled = BooleanArray(height) { y -> extendRow(pixels, inside, y * width, width) }
    val first = filled.indexOfFirst { it }
    if (first < 0) return
    val last = filled.indexOfLast { it }
    for (y in 0 until height) {
        val source = if (y < first) first else if (y > last) last else continue
        pixels.copyInto(pixels, destinationOffset = y * width, startIndex = source * width, endIndex = (source + 1) * width)
    }
}

private fun extendRow(pixels: IntArray, inside: BooleanArray, row: Int, width: Int): Boolean {
    val first = (0 until width).firstOrNull { inside[row + it] } ?: return false
    val last = (width - 1 downTo first).first { inside[row + it] }
    pixels.fill(pixels[row + first], row, row + first)
    pixels.fill(pixels[row + last], row + last + 1, row + width)
    return true
}
