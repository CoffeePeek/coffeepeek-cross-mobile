package com.coffeepeek.admin.sandbox.packscan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min

/** The bag on a soft light background with a contact shadow and a faint top sheen. */
internal fun studioCard(bag: Bitmap): Bitmap {
    val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(card)
    canvas.drawPaint(Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, CARD_HEIGHT.toFloat(), BACKGROUND_TOP, BACKGROUND_BOTTOM, Shader.TileMode.CLAMP)
    })
    val scale = min(CARD_WIDTH * BAG_SHARE / bag.width, CARD_HEIGHT * BAG_SHARE / bag.height)
    val width = bag.width * scale
    val height = bag.height * scale
    val left = (CARD_WIDTH - width) / 2
    val top = (CARD_HEIGHT - height) * 0.42f
    val bounds = RectF(left, top, left + width, top + height)
    canvas.drawRect(bounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BACKGROUND_TOP; setShadowLayer(22f, 10f, 22f, SHADOW) })
    canvas.drawBitmap(bag, null, bounds, Paint(Paint.FILTER_BITMAP_FLAG))
    canvas.drawRect(bounds, Paint().apply {
        shader = LinearGradient(left, top, left + width * 0.4f, top + height / 2.2f, SHEEN, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    })
    return card
}

private const val CARD_WIDTH = 900
private const val CARD_HEIGHT = 1100
private const val BAG_SHARE = 0.72f
private val BACKGROUND_TOP = Color.rgb(246, 243, 238)
private val BACKGROUND_BOTTOM = Color.rgb(239, 235, 229)
private val SHADOW = Color.argb(90, 60, 45, 30)
private val SHEEN = Color.argb(28, 255, 255, 255)
