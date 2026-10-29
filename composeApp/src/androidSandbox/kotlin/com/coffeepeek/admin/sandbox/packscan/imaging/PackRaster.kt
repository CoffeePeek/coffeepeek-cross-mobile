package com.coffeepeek.admin.sandbox.packscan.imaging

import com.coffeepeek.admin.sandbox.packscan.geometry.dilate
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Single-channel float image, row-major. Platform-free so the probe pipeline runs in JVM unit tests. */
internal class Plane(val width: Int, val height: Int, val data: FloatArray = FloatArray(width * height)) {
    init {
        require(data.size == width * height) { "Plane has ${data.size} values for ${width}x$height" }
    }

    operator fun get(x: Int, y: Int): Float = data[y * width + x]

    fun map(transform: (Float) -> Float): Plane = Plane(width, height, FloatArray(data.size) { transform(data[it]) })
}

/** ARGB pixels as three 0..255 channel planes. */
internal class RgbPlanes(val r: Plane, val g: Plane, val b: Plane) {
    val width: Int get() = r.width
    val height: Int get() = r.height

    fun gray(): Plane = Plane(width, height, FloatArray(r.data.size) { 0.299f * r.data[it] + 0.587f * g.data[it] + 0.114f * b.data[it] })

    fun resizeArea(width: Int, height: Int) = RgbPlanes(r.resizeArea(width, height), g.resizeArea(width, height), b.resizeArea(width, height))

    fun toPixels(): IntArray = IntArray(r.data.size) { argb(r.data[it], g.data[it], b.data[it]) }

    companion object {
        fun of(pixels: IntArray, width: Int, height: Int): RgbPlanes {
            fun channel(shift: Int) = Plane(width, height, FloatArray(pixels.size) { (pixels[it] shr shift and 0xFF).toFloat() })
            return RgbPlanes(channel(16), channel(8), channel(0))
        }
    }
}

internal fun argb(r: Float, g: Float, b: Float): Int {
    fun byte(v: Float) = v.roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (byte(r) shl 16) or (byte(g) shl 8) or byte(b)
}

/** Box downscale: every target pixel averages the source pixels whose cells fall into it (like INTER_AREA). */
internal fun Plane.resizeArea(targetWidth: Int, targetHeight: Int): Plane {
    val out = Plane(targetWidth, targetHeight)
    for (ty in 0 until targetHeight) {
        val y0 = ty * height / targetHeight
        val y1 = max(y0 + 1, (ty + 1) * height / targetHeight)
        for (tx in 0 until targetWidth) {
            val x0 = tx * width / targetWidth
            val x1 = max(x0 + 1, (tx + 1) * width / targetWidth)
            var sum = 0f
            for (y in y0 until y1) for (x in x0 until x1) sum += data[y * width + x]
            out.data[ty * targetWidth + tx] = sum / ((y1 - y0) * (x1 - x0))
        }
    }
    return out
}

/** Bilinear resize with pixel-centre alignment (like INTER_LINEAR), used to upscale model output. */
internal fun Plane.resizeBilinear(targetWidth: Int, targetHeight: Int): Plane {
    val out = Plane(targetWidth, targetHeight)
    val sx = width.toFloat() / targetWidth
    val sy = height.toFloat() / targetHeight
    for (ty in 0 until targetHeight) {
        val fy = ((ty + 0.5f) * sy - 0.5f).coerceIn(0f, height - 1f)
        val y0 = fy.toInt()
        val y1 = min(y0 + 1, height - 1)
        for (tx in 0 until targetWidth) {
            val fx = ((tx + 0.5f) * sx - 0.5f).coerceIn(0f, width - 1f)
            val x0 = fx.toInt()
            val x1 = min(x0 + 1, width - 1)
            val top = this[x0, y0] + (this[x1, y0] - this[x0, y0]) * (fx - x0)
            val bottom = this[x0, y1] + (this[x1, y1] - this[x0, y1]) * (fx - x0)
            out.data[ty * targetWidth + tx] = top + (bottom - top) * (fy - y0)
        }
    }
    return out
}

/** Separable Gaussian blur with replicated borders; [sigma] in pixels. */
internal fun Plane.gaussianBlur(sigma: Float): Plane {
    val radius = max(1, ceil(3 * sigma).toInt())
    val kernel = FloatArray(2 * radius + 1) { exp(-((it - radius) * (it - radius)) / (2 * sigma * sigma)) }
    val total = kernel.sum()
    for (i in kernel.indices) kernel[i] /= total
    return convolveRows(kernel).transposed().convolveRows(kernel).transposed()
}

/** OpenCV's sigma for a Gaussian of odd [size] when sigma is left at 0. */
internal fun sigmaForKernel(size: Int): Float = 0.3f * ((size - 1) * 0.5f - 1) + 0.8f

/** Large-radius Gaussian approximation: three box blurs (linear time whatever the sigma). */
internal fun Plane.blurLarge(sigma: Float): Plane {
    val radius = max(1, ((sqrt(4 * sigma * sigma + 1) - 1) / 2).roundToInt())
    return boxBlur(radius).boxBlur(radius).boxBlur(radius)
}

/** Mean over a (2 * [radius] + 1) square with replicated borders (like cv2.blur). */
internal fun Plane.boxBlur(radius: Int): Plane = boxRows(radius).transposed().boxRows(radius).transposed()

private fun Plane.boxRows(radius: Int): Plane {
    val out = Plane(width, height)
    val size = 2 * radius + 1
    for (y in 0 until height) {
        val row = y * width
        var sum = 0f
        for (k in -radius..radius) sum += data[row + k.coerceIn(0, width - 1)]
        for (x in 0 until width) {
            out.data[row + x] = sum / size
            sum += data[row + (x + radius + 1).coerceAtMost(width - 1)] - data[row + (x - radius).coerceAtLeast(0)]
        }
    }
    return out
}

private fun Plane.convolveRows(kernel: FloatArray): Plane {
    val radius = kernel.size / 2
    val out = Plane(width, height)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            var sum = 0f
            for (k in kernel.indices) sum += kernel[k] * data[row + (x + k - radius).coerceIn(0, width - 1)]
            out.data[row + x] = sum
        }
    }
    return out
}

private fun Plane.transposed(): Plane {
    val out = Plane(height, width)
    for (y in 0 until height) for (x in 0 until width) out.data[x * height + y] = data[y * width + x]
    return out
}

/** 3x3 Sobel derivatives with replicated borders: (d/dx, d/dy). */
internal fun Plane.sobel(): Pair<Plane, Plane> {
    val gx = Plane(width, height)
    val gy = Plane(width, height)
    for (y in 0 until height) {
        val up = max(y - 1, 0)
        val down = min(y + 1, height - 1)
        for (x in 0 until width) {
            val left = max(x - 1, 0)
            val right = min(x + 1, width - 1)
            gx.data[y * width + x] = this[right, up] + 2 * this[right, y] + this[right, down] - this[left, up] - 2 * this[left, y] - this[left, down]
            gy.data[y * width + x] = this[left, down] + 2 * this[x, down] + this[right, down] - this[left, up] - 2 * this[x, up] - this[right, up]
        }
    }
    return gx to gy
}

/** Erosion by a square of [radius]; pixels outside the image count as set (like OpenCV's default border). */
internal fun erode(mask: BooleanArray, width: Int, radius: Int): BooleanArray {
    val grown = dilate(BooleanArray(mask.size) { !mask[it] }, width, radius)
    return BooleanArray(mask.size) { !grown[it] }
}

/**
 * Two-pass chamfer distance to the nearest set pixel, with OpenCV's DIST_L2 3x3 weights.
 * Large values (no set pixel nearby) stay large.
 */
internal fun distanceTo(set: BooleanArray, width: Int, height: Int): FloatArray {
    val d = FloatArray(set.size) { if (set[it]) 0f else FAR }
    for (y in 0 until height) for (x in 0 until width) d.relax(x, y, width, height, -1)
    for (y in height - 1 downTo 0) for (x in width - 1 downTo 0) d.relax(x, y, width, height, 1)
    return d
}

// One chamfer step at (x, y) from the neighbours already visited in the pass direction.
private fun FloatArray.relax(x: Int, y: Int, width: Int, height: Int, direction: Int) {
    val i = y * width + x
    var best = this[i]
    val px = x + direction
    val py = y + direction
    if (px in 0 until width) best = min(best, this[y * width + px] + AXIS)
    if (py in 0 until height) {
        best = min(best, this[py * width + x] + AXIS)
        if (x - 1 >= 0) best = min(best, this[py * width + x - 1] + DIAGONAL)
        if (x + 1 < width) best = min(best, this[py * width + x + 1] + DIAGONAL)
    }
    this[i] = best
}

private const val FAR = 1e6f
private const val AXIS = 0.955f
private const val DIAGONAL = 1.3693f

/** Connected components: label 0 is background, labels 1..count-1 with their pixel areas and bounding boxes. */
internal class Components(val labels: IntArray, val count: Int, val areas: IntArray, val boxes: Array<IntArray>)

internal fun components(mask: BooleanArray, width: Int, eightConnected: Boolean): Components {
    val labels = IntArray(mask.size)
    val areas = mutableListOf(0)
    val boxes = mutableListOf(intArrayOf(0, 0, 0, 0))
    val queue = IntArray(mask.size)
    for (start in mask.indices) {
        if (!mask[start] || labels[start] != 0) continue
        val box = flood(mask, labels, queue, start, width, areas.size, eightConnected)
        areas.add(box[4])
        boxes.add(box.copyOf(4))
    }
    return Components(labels, areas.size, areas.toIntArray(), boxes.toTypedArray())
}

// Breadth-first fill from `start`; returns (left, top, right, bottom, area) with exclusive right/bottom.
private fun flood(mask: BooleanArray, labels: IntArray, queue: IntArray, start: Int, width: Int, label: Int, eight: Boolean): IntArray {
    val box = intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE, 0, 0, 0)
    var head = 0
    var tail = 0
    queue[tail++] = start
    labels[start] = label
    while (head < tail) {
        val i = queue[head++]
        val x = i % width
        val y = i / width
        box[0] = min(box[0], x); box[1] = min(box[1], y); box[2] = max(box[2], x + 1); box[3] = max(box[3], y + 1); box[4]++
        for (dy in -1..1) for (dx in -1..1) {
            if ((dx == 0 && dy == 0) || (!eight && dx != 0 && dy != 0)) continue
            val nx = x + dx
            val j = i + dy * width + dx
            if (nx !in 0 until width || j !in mask.indices || !mask[j] || labels[j] != 0) continue
            labels[j] = label
            queue[tail++] = j
        }
    }
    return box
}

/** Linear-interpolated percentile of [values] (like numpy's default); [q] in 0..100. */
internal fun percentile(values: FloatArray, q: Float): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sortedArray()
    val position = q / 100f * (sorted.size - 1)
    val low = floor(position).toInt()
    val high = min(low + 1, sorted.size - 1)
    return sorted[low] + (sorted[high] - sorted[low]) * (position - low)
}
