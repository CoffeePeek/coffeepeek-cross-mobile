package com.coffeepeek.admin.sandbox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import com.coffeepeek.admin.sandbox.backend.SandboxBagCard
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

/** A synthetic bag front, portrait like a scanned card: a coloured pack with a label naming the coffee, roaster and weight. */
internal fun sandboxBagCardBytes(card: SandboxBagCard): ByteArray {
    val bitmap = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val tone = sandboxBagColor(card.tone)
    canvas.drawColor(tone)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 0, 0, 0) }
    canvas.drawRect(0f, 0f, 480f, 56f, paint)
    paint.apply { color = Color.argb(120, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 4f }
    canvas.drawCircle(240f, 120f, 18f, paint)
    paint.apply { color = Color.rgb(244, 234, 215); style = Paint.Style.FILL }
    canvas.drawRoundRect(56f, 190f, 424f, 520f, 18f, 18f, paint)
    canvas.drawBagLabel(card, tone)
    return ByteArrayOutputStream().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream))
        stream.toByteArray().also { bitmap.recycle() }
    }
}

/** The coffee name in the pack's colour, the roaster under it, the weight at the bottom of the pack. */
private fun Canvas.drawBagLabel(card: SandboxBagCard, tone: Int) {
    val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = tone; textSize = 44f; typeface = Typeface.DEFAULT_BOLD }
    val title = StaticLayout.Builder.obtain(card.title, 0, card.title.length, text, 312).setMaxLines(4).build()
    save()
    translate(84f, 222f)
    title.draw(this)
    restore()
    text.apply { textSize = 26f; typeface = Typeface.DEFAULT; color = Color.rgb(87, 83, 78) }
    card.roaster?.let { drawText(it, 84f, 222f + title.height + 48f, text) }
    text.apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER }
    card.weight?.let { drawText(it, 240f, 590f, text) }
}

private fun sandboxBagColor(tone: Int): Int = listOf(
    Color.rgb(47, 74, 58),
    Color.rgb(140, 63, 46),
    Color.rgb(32, 58, 92),
    Color.rgb(166, 124, 46),
    Color.rgb(88, 52, 110),
    Color.rgb(28, 28, 30),
    Color.rgb(46, 110, 104),
)[tone.mod(7)]
