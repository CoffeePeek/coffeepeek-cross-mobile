package com.coffeepeek.feature.shelf.data.scan.imaging

import kotlin.math.max

/** Softened bag pixels and the share of the bag under strong broad glare (a pre-moderation signal). */
internal class SoftenResult(val pixels: IntArray, val strongGlareShare: Float)

/**
 * Gentle shading/glare softening that keeps the crumpled-film texture. The photo's deviation from the
 * estimated ink is split by scale: the smooth part (light falloff, broad shadow and glare) is reduced, the
 * fine part (creases, film texture) mostly kept; glare is reduced more than shadow. Text and details take
 * the same smooth correction as the ink around them.
 */
internal fun soften(pixels: IntArray, width: Int, height: Int, keep: SoftenStrength = SoftenStrength.Strong): SoftenResult {
    val lab = LabPlanes.of(RgbPlanes.of(pixels, width, height))
    val (ink, known) = estimateInk(lab).let { it.ink to it.known }
    val sigma = SMOOTH * max(width, height)
    val weight = Plane(width, height, FloatArray(known.size) { if (known[it]) 1f else 0f }).blurLarge(sigma)
    val channels = listOf(lab.l to ink.l, lab.a to ink.a, lab.b to ink.b)
    val smooth = channels.map { (photo, estimate) ->
        val deviation = Plane(width, height, FloatArray(known.size) { if (known[it]) photo.data[it] - estimate.data[it] else 0f })
        val blurred = deviation.blurLarge(sigma)
        Plane(width, height, FloatArray(known.size) {
            blurred.data[it] / max(weight.data[it], 1e-3f) * (weight.data[it] / 0.2f).coerceIn(0f, 1f)
        }) to deviation
    }
    val out = lab.copy()
    val outChannels = listOf(out.l, out.a, out.b)
    for (i in known.indices) {
        val reduce = 1f - if (smooth[0].first.data[i] > 0f) keep.glare else keep.shadow
        for (c in 0 until 3) {
            val broad = smooth[c].first.data[i]
            val fine = if (known[i]) smooth[c].second.data[i] - broad else 0f
            outChannels[c].data[i] -= broad * reduce + fine * (1f - keep.texture)
        }
    }
    val glare = smooth[0].first.data.count { it > STRONG_GLARE }.toFloat() / known.size
    return SoftenResult(out.toRgb().toPixels(), glare)
}

/** Shares kept of broad shadow, broad glare and fine texture; [Strong] is the agreed preset. */
internal enum class SoftenStrength(val shadow: Float, val glare: Float, val texture: Float) {
    Strong(0.25f, 0.15f, 0.75f),
}

private const val SMOOTH = 0.035f
private const val STRONG_GLARE = 25f
