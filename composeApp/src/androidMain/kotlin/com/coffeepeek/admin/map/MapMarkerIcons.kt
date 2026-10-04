package com.coffeepeek.admin.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.coffeepeek.R
import com.coffeepeek.domain.model.CoffeeShopType
import java.util.ArrayDeque
import kotlin.math.roundToInt

internal enum class MapPinVisual {
    Default,
    Selected,
    Detail,
}

internal object MapMarkerIcons {

    private const val RENDER_SCALE = 1f

    const val PIN_DEFAULT_DP = 31f
    const val PIN_SELECTED_DP = 41f
    const val PIN_DETAIL_DP = 43f

    private const val WHITE_STROKE_DP = 2.5f
    private const val RING_DEFAULT_DP = 1.1f
    private const val RING_SELECTED_DP = 1.6f
    private const val SELECTED_HALO_STROKE_DP = 2f
    private const val SELECTED_HALO_GAP_DP = 1.5f
    private const val CLUSTER_STROKE_DP = 2f
    private const val CLUSTER_TEXT_SP = 13f

    private const val BRAND_PRIMARY = 0xFFEAB308.toInt()
    private const val BRAND_PRIMARY_DARK = 0xFFCA8A04.toInt()
    private const val DARK_BACKGROUND = 0xFF1A1412.toInt()
    private const val DARK_BORDER = 0xFF3D2F28.toInt()
    private const val LIGHT_TEXT_SECONDARY = 0xFF78716C.toInt()
    private const val DARK_BORDER_HOVER = 0xFF4A3D35.toInt()
    private const val TEXT_ON_PRIMARY = 0xFF1A1412.toInt()
    private const val MY_LOCATION = 0xFF2563EB.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SHADOW = 0xFF1A1412.toInt()

    private val cache = mutableMapOf<String, Bitmap>()
    private val knockoutCache = mutableMapOf<Int, Bitmap>()
    private var cachedClusterTypeface: Typeface? = null

    fun pinSizeDp(visual: MapPinVisual): Float = when (visual) {
        MapPinVisual.Default -> PIN_DEFAULT_DP
        MapPinVisual.Selected -> PIN_SELECTED_DP
        MapPinVisual.Detail -> PIN_DETAIL_DP
    }

    fun pinBitmap(
        context: Context,
        type: String,
        visual: MapPinVisual,
    ): Bitmap {
        val key = "pin-$type-${visual.name}"
        return cache.getOrPut(key) {
            createPinBitmap(context.applicationContext, type, visual)
        }
    }

    fun clusterBitmap(context: Context, count: Int): Bitmap {
        if (clusterUsesShopPoint(count)) {
            return pinBitmap(context, CoffeeShopType.SPECIALTY, MapPinVisual.Default)
        }
        val label = clusterCountLabel(count)
        val diameter = clusterDiameterDp(count)
        val key = "cluster-$label-${diameter.toInt()}"
        return cache.getOrPut(key) {
            createClusterBitmap(context.applicationContext, label, diameter)
        }
    }

    fun zoneBitmap(context: Context, name: String, count: Int, isDarkTheme: Boolean, color: String): Bitmap {
        val shortName = if (name.length > 18) "${name.take(17)}…" else name
        val label = "$shortName · $count"
        val key = "zone-$isDarkTheme-$color-$label"
        return cache.getOrPut(key) {
            createZoneBitmap(context.applicationContext, label, isDarkTheme, color)
        }
    }

    fun myLocationBitmap(context: Context): Bitmap = cache.getOrPut("my-location") {
        val density = context.resources.displayMetrics.density
        val size = (38f * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        canvas.drawCircle(
            center,
            center,
            17f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ColorUtils.setAlphaComponent(MY_LOCATION, 0x33)
            },
        )
        canvas.drawCircle(
            center,
            center,
            10f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = MY_LOCATION
            },
        )
        canvas.drawCircle(
            center,
            center,
            10f * density - 1.8f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3.6f * density
                color = WHITE
            },
        )
        bitmap
    }

    private fun pinStyle(type: String): PinStyle = when (type) {
        CoffeeShopType.SPECIALTY -> PinStyle(
            fill = BRAND_PRIMARY,
            ring = BRAND_PRIMARY_DARK,
            mascotRes = R.drawable.maskot_with_bean,
        )
        CoffeeShopType.CAFE -> PinStyle(
            fill = LIGHT_TEXT_SECONDARY,
            ring = DARK_BORDER_HOVER,
            mascotRes = R.drawable.maskot_with_dessert,
        )
        else -> PinStyle(
            fill = DARK_BACKGROUND,
            ring = DARK_BORDER,
            mascotRes = R.drawable.maskot_with_cup,
        )
    }

    private fun createPinBitmap(context: Context, type: String, visual: MapPinVisual): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = density * RENDER_SCALE
        val size = pinSizeDp(visual) * px
        val selected = visual != MapPinVisual.Default
        val haloExtra = if (selected) (SELECTED_HALO_GAP_DP + SELECTED_HALO_STROKE_DP) * px else 0f
        val shadowPad = (if (selected) 10f else 4f) * px
        val pad = shadowPad + haloExtra
        val width = (size + pad * 2f).roundToInt().coerceAtLeast(1)
        val height = width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f
        val pinColors = pinStyle(type)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = pinColors.fill
            if (selected) {
                setShadowLayer(8f * px, 0f, 2f * px, ColorUtils.setAlphaComponent(SHADOW, (0.35f * 255).toInt()))
            } else {
                setShadowLayer(3f * px, 0f, 1f * px, ColorUtils.setAlphaComponent(SHADOW, (0.32f * 255).toInt()))
            }
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val mascot = knockoutMascot(context, pinColors.mascotRes)
        if (mascot != null) {
            val drawSize = size * 1.72f
            val offsetY = size * 0.08f
            val left = cx - drawSize / 2f
            val top = cy - drawSize * 0.58f + offsetY
            canvas.save()
            val clip = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
            canvas.clipPath(clip)
            canvas.drawBitmap(
                mascot,
                Rect(0, 0, mascot.width, mascot.height),
                RectF(left, top, left + drawSize, top + drawSize),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            canvas.restore()
        }

        val whiteStroke = WHITE_STROKE_DP * px
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = whiteStroke
            color = WHITE
        }
        canvas.drawCircle(cx, cy, radius - whiteStroke / 2f, whitePaint)

        val ringStroke = (if (selected) RING_SELECTED_DP else RING_DEFAULT_DP) * px
        val ringAlpha = if (selected) 0.95f else 0.70f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = ringStroke
            color = ColorUtils.setAlphaComponent(pinColors.ring, (ringAlpha * 255).toInt())
        }
        canvas.drawCircle(cx, cy, radius - ringStroke / 2f, ringPaint)

        if (selected) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = SELECTED_HALO_STROKE_DP * px
                color = ColorUtils.setAlphaComponent(BRAND_PRIMARY, 0x99)
            }
            canvas.drawCircle(
                cx,
                cy,
                radius + SELECTED_HALO_GAP_DP * px + (SELECTED_HALO_STROKE_DP * px) / 2f,
                haloPaint,
            )
        }

        return bitmap
    }

    private fun createClusterBitmap(context: Context, label: String, diameterDp: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = density * RENDER_SCALE
        val size = diameterDp * px
        val pad = 12f * px
        val width = (size + pad * 2f).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = width / 2f
        val cy = width / 2f
        val radius = size / 2f

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(BRAND_PRIMARY, (0.92f * 255).toInt())
            setShadowLayer(10f * px, 0f, 2f * px, ColorUtils.setAlphaComponent(SHADOW, (0.35f * 255).toInt()))
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * px
            color = ColorUtils.setAlphaComponent(SHADOW, (0.10f * 255).toInt())
        }
        canvas.drawCircle(cx, cy, radius, outlinePaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = CLUSTER_STROKE_DP * px
            color = WHITE
        }
        canvas.drawCircle(cx, cy, radius - CLUSTER_STROKE_DP * px / 2f, strokePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_ON_PRIMARY
            textAlign = Paint.Align.CENTER
            textSize = CLUSTER_TEXT_SP * px
            typeface = clusterTypeface(context)
            letterSpacing = -0.02f
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)
        return bitmap
    }

    private fun createZoneBitmap(context: Context, label: String, isDarkTheme: Boolean, zoneColor: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFE6DDD3.toInt() else 0xFF4A4038.toInt()
            textSize = 12f * density
            typeface = clusterTypeface(context)
        }
        val horizontalPadding = 8f * density
        val verticalPadding = 4f * density
        val height = textPaint.fontMetrics.run { bottom - top } + verticalPadding * 2f
        val width = textPaint.measureText(label) + horizontalPadding * 2f
        val shadowPadding = 5f * density
        val bitmap = Bitmap.createBitmap(
            (width + shadowPadding * 2f).roundToInt().coerceAtLeast(1),
            (height + shadowPadding * 2f).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val rect = RectF(
            shadowPadding,
            shadowPadding,
            shadowPadding + width,
            shadowPadding + height,
        )
        canvas.drawRoundRect(
            rect,
            height / 2f,
            height / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                // Muted chip: near-surface fill so the label reads as context, not as a pin.
                color = if (isDarkTheme) 0xEB2A221D.toInt() else 0xEBFFFFFF.toInt()
                setShadowLayer(3f * density, 0f, 1f * density, ColorUtils.setAlphaComponent(SHADOW, 0x1F))
            },
        )
        canvas.drawRoundRect(
            rect,
            height / 2f,
            height / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                color = ColorUtils.setAlphaComponent(Color.parseColor(zoneColor), 0xCC)
            },
        )
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, rect.left + horizontalPadding, textY, textPaint)
        return bitmap
    }

    private fun knockoutMascot(context: Context, resId: Int): Bitmap? {
        knockoutCache[resId]?.let { return it }
        val source = BitmapFactory.decodeResource(context.resources, resId) ?: return null
        val knocked = knockoutBlackBackground(source)
        if (knocked !== source) source.recycle()
        knockoutCache[resId] = knocked
        return knocked
    }

    internal fun knockoutBlackBackground(source: Bitmap): Bitmap {
        val bmp = if (source.isMutable && source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val width = bmp.width
        val height = bmp.height
        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)
        val visited = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()

        fun enqueue(index: Int) {
            if (visited[index]) return
            visited[index] = true
            queue.add(index)
        }

        fun isBackground(color: Int): Boolean {
            val alpha = Color.alpha(color)
            if (alpha <= 8) return false
            val luma = 0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)
            return luma < 40f
        }

        for (x in 0 until width) {
            enqueue(x)
            enqueue((height - 1) * width + x)
        }
        for (y in 0 until height) {
            enqueue(y * width)
            enqueue(y * width + (width - 1))
        }

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            if (!isBackground(pixels[index])) continue
            pixels[index] = Color.TRANSPARENT
            val x = index % width
            val y = index / width
            if (x > 0) enqueue(index - 1)
            if (x < width - 1) enqueue(index + 1)
            if (y > 0) enqueue(index - width)
            if (y < height - 1) enqueue(index + width)
        }

        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    private fun clusterTypeface(context: Context): Typeface {
        cachedClusterTypeface?.let { return it }
        val loaded = runCatching {
            val cacheFile = java.io.File(context.cacheDir, "manrope_extrabold.ttf")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val stream = sequenceOf(
                    "fonts/manrope_extrabold.ttf",
                    "composeResources/coffeepeek.composeapp.generated.resources/font/manrope_extrabold.ttf",
                    "font/manrope_extrabold.ttf",
                ).mapNotNull { path ->
                    runCatching { context.assets.open(path) }.getOrNull()
                        ?: MapMarkerIcons::class.java.getResourceAsStream("/$path")
                }.firstOrNull()
                stream?.use { input -> cacheFile.outputStream().use { input.copyTo(it) } }
            }
            if (cacheFile.exists() && cacheFile.length() > 0L) Typeface.createFromFile(cacheFile) else null
        }.getOrNull()
        val typeface = loaded ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        cachedClusterTypeface = typeface
        return typeface
    }

    private data class PinStyle(
        val fill: Int,
        val ring: Int,
        val mascotRes: Int,
    )
}
