package com.coffeepeek.admin.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.DisplayMetrics
import kotlin.math.roundToInt
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.VectorSource

private const val SUBWAY_LAYER = "cp-subway-stations"
private const val SUBWAY_ICON = "cp-subway-badge"
private const val TILE_SOURCE = "openmaptiles"

internal fun addSubwayStations(style: Style, metrics: DisplayMetrics, dark: Boolean) {
    if (style.getSourceAs<VectorSource>(TILE_SOURCE) == null) return
    style.addImage(SUBWAY_ICON, subwayBadge(metrics))
    val stations = SymbolLayer(SUBWAY_LAYER, TILE_SOURCE)
        .withSourceLayer("poi")
        .withFilter(eq(get("subclass"), literal("subway")))
        .withProperties(
            iconImage(SUBWAY_ICON),
            textField(get("name")),
            textFont(arrayOf("Noto Sans Regular")),
            textSize(12f),
            textOffset(arrayOf(0f, 1.2f)),
            textOptional(true),
            textColor(if (dark) "#AAC9E0" else "#365872"),
            textHaloColor(if (dark) "#1A1412" else "#FAF8F5"),
            textHaloWidth(1.2f),
        )
    stations.minZoom = 12f
    style.addLayer(stations)
}

private fun subwayBadge(metrics: DisplayMetrics): Bitmap {
    val size = (18 * metrics.density).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.density = metrics.densityDpi
    val canvas = Canvas(bitmap)
    canvas.scale(size / 18f, size / 18f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#446984") }
    canvas.drawCircle(9f, 9f, 8.5f, paint)
    paint.color = Color.WHITE
    paint.textSize = 12f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("M", 9f, 9f - (paint.ascent() + paint.descent()) / 2, paint)
    return bitmap
}
