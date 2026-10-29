package com.coffeepeek.feature.shelf.data.scan.imaging

import com.coffeepeek.feature.shelf.data.scan.geometry.dilate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Estimated ink colour per pixel and where that estimate exists (large ink areas and their glare/shadow). */
internal class InkEstimate(val ink: LabPlanes, val known: BooleanArray)

/**
 * Splits the bag into regions bounded by sharp print edges (glare and shading change softly, ink edges
 * are sharp) and gives every large region its typical colour. Glare and shadow patches join the base ink
 * or the largest ink they touch: same hue, no extra saturation, and either a mild lightness change or a
 * soft border. Small regions (text, drawings) keep no estimate.
 */
internal fun estimateInk(lab: LabPlanes): InkEstimate {
    val regions = inkRegions(lab)
    val grad = lab.l.gaussianBlur(0.7f).sobel().let { (gx, gy) -> Plane(lab.width, lab.height, FloatArray(gx.data.size) { hypot(gx.data[it], gy.data[it]) / 8 }) }
    val members = regionMembers(regions)
    val dominant = (1 until regions.count).maxByOrNull { regions.areas[it] } ?: return InkEstimate(lab.copy(), BooleanArray(lab.l.data.size))
    val base = typical(lab, members[dominant])
    val assigned = HashMap<Int, FloatArray>()
    for (k in (1 until regions.count).sortedByDescending { regions.areas[it] }) {
        if (regions.areas[k] < MIN_PIXELS) break
        val color = if (k == dominant) base else parentInk(k, typical(lab, members[k]), base, assigned, regions, lab, grad) ?: continue
        assigned[k] = color
    }
    return paint(lab, regions, assigned)
}

// The ink a region belongs to: a larger neighbour or the base when it is their glare/shadow, itself when large.
private fun parentInk(
    k: Int, color: FloatArray, base: FloatArray, assigned: Map<Int, FloatArray>,
    regions: Components, lab: LabPlanes, grad: Plane,
): FloatArray? {
    val big = regions.areas[k] >= MIN_REGION * lab.l.data.size
    val parents = listOfNotNull(largestNeighbour(regions, k, lab.width, assigned)?.let { assigned[it] }, base)
    val parent = parents.firstOrNull { sameMaterial(color, it, !big) { edgeSharpness(regions, k, lab, grad, color[0] - it[0]) } }
    return parent ?: color.takeIf { big }
}

private fun inkRegions(lab: LabPlanes): Components {
    val light = lab.l.map { (it * 2.55f).coerceIn(0f, 255f) }.gaussianBlur(1f)
    val chroma = Plane(lab.width, lab.height, FloatArray(lab.l.data.size) { (hypot(lab.a.data[it], lab.b.data[it]) * 3).coerceIn(0f, 255f) }).gaussianBlur(1f)
    val lightEdges = light.canny(24f, 60f)
    val chromaEdges = chroma.canny(24f, 60f)
    val edges = dilate(BooleanArray(lightEdges.size) { lightEdges[it] || chromaEdges[it] }, lab.width, 1)
    return components(BooleanArray(edges.size) { !edges[it] }, lab.width, eightConnected = false)
}

private fun regionMembers(regions: Components): Array<IntArray> {
    val members = Array(regions.count) { IntArray(regions.areas[it]) }
    val fill = IntArray(regions.count)
    for (i in regions.labels.indices) regions.labels[i].let { if (it > 0) members[it][fill[it]++] = i }
    return members
}

/**
 * Typical colour of a region. Shading darkens and glare brightens: on light inks shading dominates, so an
 * upper lightness level is used; on dark inks glare dominates, so the mode. Near-neutral colours turn neutral.
 */
internal fun typical(lab: LabPlanes, pixels: IntArray): FloatArray {
    val lights = FloatArray(pixels.size) { lab.l.data[pixels[it]] }
    val mode = lightnessMode(lights)
    val light = ((mode - 30f) / 30f).coerceIn(0f, 1f)
    val level = mode * (1 - light) + percentile(lights, 75f) * light
    val near = pixels.filter { abs(lab.l.data[it] - mode) < NEAR_MODE }.ifEmpty { pixels.toList() }
    var a = percentile(FloatArray(near.size) { lab.a.data[near[it]] }, 50f)
    var b = percentile(FloatArray(near.size) { lab.b.data[near[it]] }, 50f)
    if (hypot(a, b) < 6f) {
        a *= 0.3f
        b *= 0.3f
    }
    return floatArrayOf(level, a, b)
}

// Centre of the most populated 2-unit lightness bin after a 5-bin moving average.
private fun lightnessMode(lights: FloatArray): Float {
    val histogram = IntArray(50)
    for (l in lights) histogram[(l / 2f).toInt().coerceIn(0, 49)]++
    val smooth = FloatArray(50) { i -> (i - 2..i + 2).sumOf { j -> if (j in 0 until 50) histogram[j] else 0 } / 5f }
    return smooth.indices.maxBy { smooth[it] } * 2f + 1f
}

/** Is [color] a glare or shadow patch of the [base] ink rather than a different ink? */
internal fun sameMaterial(color: FloatArray, base: FloatArray, small: Boolean, sharpness: () -> Float): Boolean {
    if (!sameHue(color, base)) return false
    if (hypot(color[1], color[2]) > hypot(base[1], base[2]) * 1.4f + 4f) return false // shading never adds saturation
    val dl = color[0] - base[0]
    if ((dl > -20f && dl <= 0f) || (dl > 0f && dl < (if (small) 30f else 45f) && color[0] < 78f)) return true
    return dl > 0f && sharpness() < SOFT_EDGE
}

private fun sameHue(color: FloatArray, base: FloatArray): Boolean {
    val c1 = hypot(color[1], color[2])
    val c2 = hypot(base[1], base[2])
    if (c1 < 8f && c2 < 8f) return true
    if (c1 < 8f || c2 < 8f) return false
    val diff = abs(atan2(color[2], color[1]) - atan2(base[2], base[1]))
    return min(diff, (2 * PI).toFloat() - diff) < radians(20.0).toFloat()
}

/** Steepness of a region's border per unit of contrast: print ~0.3, glare ~0.06. */
private fun edgeSharpness(regions: Components, k: Int, lab: LabPlanes, grad: Plane, contrast: Float): Float {
    val ring = ringAround(regions, k, lab.width, lab.height, margin = 3, radius = 2) { true }
    if (ring.isEmpty()) return 1f
    return percentile(FloatArray(ring.size) { grad.data[ring[it]] }, 75f) / max(contrast, 1f)
}

// The assigned region sharing the longest border with region k across the edge band.
private fun largestNeighbour(regions: Components, k: Int, width: Int, assigned: Map<Int, FloatArray>): Int? {
    val ring = ringAround(regions, k, width, regions.labels.size / width, margin = 5, radius = 4) { regions.labels[it] > 0 }
    return ring.groupingBy { regions.labels[it] }.eachCount().filterKeys { it in assigned }.maxByOrNull { it.value }?.key
}

// Pixels within [radius] of region k (not in it), searched in its bounding box grown by [margin].
private fun ringAround(regions: Components, k: Int, width: Int, height: Int, margin: Int, radius: Int, accept: (Int) -> Boolean): List<Int> {
    val box = regions.boxes[k]
    val x0 = max(0, box[0] - margin)
    val y0 = max(0, box[1] - margin)
    val w = min(width, box[2] + margin) - x0
    val h = min(height, box[3] + margin) - y0
    val inside = BooleanArray(w * h) { regions.labels[(y0 + it / w) * width + x0 + it % w] == k }
    val grown = dilate(inside, w, radius)
    return (0 until w * h).filter { grown[it] && !inside[it] }.map { (y0 + it / w) * width + x0 + it % w }.filter(accept)
}

private fun paint(lab: LabPlanes, regions: Components, assigned: Map<Int, FloatArray>): InkEstimate {
    val ink = lab.copy()
    val known = BooleanArray(lab.l.data.size)
    for (i in known.indices) {
        val color = assigned[regions.labels[i]] ?: continue
        ink.l.data[i] = color[0]; ink.a.data[i] = color[1]; ink.b.data[i] = color[2]
        known[i] = true
    }
    return InkEstimate(ink, known)
}

private const val MIN_PIXELS = 20
private const val MIN_REGION = 0.002f
private const val NEAR_MODE = 12f
private const val SOFT_EDGE = 0.12f
