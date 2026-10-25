package com.coffeepeek.admin.sandbox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream

/** Synthetic fixture, never a bundled private photo or a remote image. */
internal fun sandboxImageBytes(variant: Int = 0): ByteArray {
    val bitmap = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(sandboxPhotoColor(variant))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawRoundRect(190f, 100f, 420f, 320f, 50f, 50f, paint)
    paint.style = Paint.Style.STROKE; paint.strokeWidth = 24f
    canvas.drawCircle(430f, 190f, 65f, paint)
    paint.color = Color.rgb(245, 185, 0)
    canvas.drawLine(240f, 40f, 240f, 80f, paint)
    canvas.drawLine(310f, 25f, 310f, 80f, paint)
    return ByteArrayOutputStream().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        stream.toByteArray().also { bitmap.recycle() }
    }
}

private fun sandboxPhotoColor(variant: Int): Int = listOf(
    Color.rgb(92, 45, 145),
    Color.rgb(113, 74, 46),
    Color.rgb(36, 94, 113),
    Color.rgb(76, 113, 72),
    Color.rgb(153, 71, 57),
)[variant.mod(5)]
