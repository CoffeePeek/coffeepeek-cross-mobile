package com.coffeepeek.admin.sandbox.packscan.imaging

import kotlin.math.cbrt
import kotlin.math.pow

/** CIE Lab planes (L 0..100, a/b roughly -128..127), D65 white, same constants as OpenCV's float conversion. */
internal class LabPlanes(val l: Plane, val a: Plane, val b: Plane) {
    val width: Int get() = l.width
    val height: Int get() = l.height

    fun toRgb(): RgbPlanes {
        val r = Plane(width, height)
        val g = Plane(width, height)
        val bl = Plane(width, height)
        val rgb = FloatArray(3)
        for (i in l.data.indices) {
            labToRgb(l.data[i], a.data[i], b.data[i], rgb)
            r.data[i] = rgb[0]; g.data[i] = rgb[1]; bl.data[i] = rgb[2]
        }
        return RgbPlanes(r, g, bl)
    }

    fun copy() = LabPlanes(Plane(width, height, l.data.copyOf()), Plane(width, height, a.data.copyOf()), Plane(width, height, b.data.copyOf()))

    companion object {
        fun of(rgb: RgbPlanes): LabPlanes {
            val out = LabPlanes(Plane(rgb.width, rgb.height), Plane(rgb.width, rgb.height), Plane(rgb.width, rgb.height))
            val lab = FloatArray(3)
            for (i in rgb.r.data.indices) {
                rgbToLab(rgb.r.data[i], rgb.g.data[i], rgb.b.data[i], lab)
                out.l.data[i] = lab[0]; out.a.data[i] = lab[1]; out.b.data[i] = lab[2]
            }
            return out
        }
    }
}

/** sRGB 0..255 to Lab, written into [out]. */
internal fun rgbToLab(r: Float, g: Float, b: Float, out: FloatArray) {
    val lr = linear(r / 255f)
    val lg = linear(g / 255f)
    val lb = linear(b / 255f)
    val x = (0.412453f * lr + 0.357580f * lg + 0.180423f * lb) / WHITE_X
    val y = 0.212671f * lr + 0.715160f * lg + 0.072169f * lb
    val z = (0.019334f * lr + 0.119193f * lg + 0.950227f * lb) / WHITE_Z
    val fy = f(y)
    out[0] = if (y > EPSILON) 116f * fy - 16f else 903.3f * y
    out[1] = 500f * (f(x) - fy)
    out[2] = 200f * (fy - f(z))
}

/** Lab to sRGB 0..255 (clamped), written into [out]. */
internal fun labToRgb(l: Float, a: Float, b: Float, out: FloatArray) {
    val fy = (l + 16f) / 116f
    val fx = fy + a / 500f
    val fz = fy - b / 200f
    val y = if (l > 7.9996f) fy * fy * fy else l / 903.3f
    val x = inverse(fx) * WHITE_X
    val z = inverse(fz) * WHITE_Z
    out[0] = gamma(3.240479f * x - 1.537150f * y - 0.498535f * z)
    out[1] = gamma(-0.969256f * x + 1.875991f * y + 0.041556f * z)
    out[2] = gamma(0.055648f * x - 0.204043f * y + 1.057311f * z)
}

private fun linear(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

private fun gamma(c: Float): Float {
    val v = c.coerceIn(0f, 1f)
    return 255f * if (v <= 0.0031308f) 12.92f * v else 1.055f * v.pow(1f / 2.4f) - 0.055f
}

private fun f(t: Float): Float = if (t > EPSILON) cbrt(t) else 7.787f * t + 16f / 116f

private fun inverse(f: Float): Float {
    val cube = f * f * f
    return if (cube > EPSILON) cube else (f - 16f / 116f) / 7.787f
}

private const val EPSILON = 0.008856f
private const val WHITE_X = 0.950456f
private const val WHITE_Z = 1.088754f
