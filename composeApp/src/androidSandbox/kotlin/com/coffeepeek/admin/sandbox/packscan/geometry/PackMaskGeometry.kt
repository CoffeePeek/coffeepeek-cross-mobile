package com.coffeepeek.admin.sandbox.packscan.geometry

/** Subject confidence mask, row-major from the top-left corner. Platform-free for unit tests. */
internal class PackMask(val width: Int, val height: Int, private val confidence: FloatArray) {
    init {
        require(confidence.size == width * height) { "Mask has ${confidence.size} values for ${width}x$height" }
    }

    fun isSubject(x: Int, y: Int): Boolean = confidence[y * width + x] > SUBJECT_CONFIDENCE

    val area: Int get() = confidence.count { it > SUBJECT_CONFIDENCE }

    private companion object {
        const val SUBJECT_CONFIDENCE = 0.5f
    }
}

internal data class PackPoint(val x: Float, val y: Float)

/**
 * Convex outline of the subject in mask pixels. A coffee bag is nearly convex, so the hull
 * restores edges the segmenter bites out on glare and folds.
 */
internal fun PackMask.convexHull(): List<PackPoint> = monotoneChain(rowEdges())

// Each subject row contributes the outer corners of its leftmost and rightmost pixels.
private fun PackMask.rowEdges(): List<PackPoint> = buildList {
    for (y in 0 until height) {
        val left = (0 until width).firstOrNull { isSubject(it, y) } ?: continue
        val right = (width - 1 downTo left).first { isSubject(it, y) } + 1
        for (edgeY in listOf(y, y + 1)) {
            add(PackPoint(left.toFloat(), edgeY.toFloat()))
            add(PackPoint(right.toFloat(), edgeY.toFloat()))
        }
    }
}

private fun monotoneChain(points: List<PackPoint>): List<PackPoint> {
    val sorted = points.distinct().sortedWith(compareBy({ it.x }, { it.y }))
    if (sorted.size < 3) return sorted
    return halfHull(sorted).dropLast(1) + halfHull(sorted.asReversed()).dropLast(1)
}

private fun halfHull(points: List<PackPoint>): List<PackPoint> {
    val hull = ArrayList<PackPoint>()
    for (point in points) {
        while (hull.size >= 2 && cross(hull[hull.size - 2], hull.last(), point) <= 0f) hull.removeAt(hull.lastIndex)
        hull.add(point)
    }
    return hull
}

private fun cross(origin: PackPoint, a: PackPoint, b: PackPoint): Float =
    (a.x - origin.x) * (b.y - origin.y) - (a.y - origin.y) * (b.x - origin.x)
