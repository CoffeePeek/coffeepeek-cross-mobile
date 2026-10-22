package com.coffeepeek.admin.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.coffeepeek.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.domain.model.CoffeeShopType
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

    private const val RING_DEFAULT_DP = 1.1f
    private const val SELECTED_HALO_STROKE_DP = 2f
    private const val SELECTED_HALO_GAP_DP = 1.5f
    private const val CLUSTER_STROKE_DP = 2f
    private const val CLUSTER_TEXT_SP = 13f

    private const val BRAND_PRIMARY = 0xFFEAB308.toInt()
    private const val TEXT_ON_PRIMARY = 0xFF1A1412.toInt()
    private const val MY_LOCATION = 0xFF2563EB.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SHADOW = 0xFF1A1412.toInt()

    private val cache = mutableMapOf<String, Bitmap>()
    private val iconCache = mutableMapOf<Int, Bitmap>()
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
        isDarkTheme: Boolean,
    ): Bitmap {
        val key = "pin-$type-${visual.name}-$isDarkTheme"
        return cache.getOrPut(key) {
            createPinBitmap(context.applicationContext, type, visual, isDarkTheme)
        }
    }

    fun clusterBitmap(context: Context, count: Int, isDarkTheme: Boolean): Bitmap {
        if (clusterUsesShopPoint(count)) {
            return pinBitmap(context, CoffeeShopType.SPECIALTY, MapPinVisual.Default, isDarkTheme)
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

    private fun pinIcon(type: String): Int = when (type) {
        CoffeeShopType.SPECIALTY -> R.drawable.map_coffeepeek_bean
        CoffeeShopType.CAFE -> R.drawable.map_fork_knife
        else -> R.drawable.map_cup_fill
    }

    private fun createPinBitmap(context: Context, type: String, visual: MapPinVisual, dark: Boolean): Bitmap {
        val px = context.resources.displayMetrics.density * RENDER_SCALE
        val size = pinSizeDp(visual) * px
        val selected = visual != MapPinVisual.Default
        val haloExtra = if (selected) (SELECTED_HALO_GAP_DP + SELECTED_HALO_STROKE_DP) * px else 0f
        val pad = (if (selected) 10f else 4f) * px + haloExtra
        val width = (size + pad * 2f).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = width / 2f
        drawPinBackground(canvas, center, size / 2f, px, selected, dark)
        drawPinIcon(context, canvas, pinIcon(type), center, size, dark)
        drawPinRing(canvas, center, size / 2f, px, selected, dark)
        return bitmap
    }

    private fun drawPinBackground(canvas: Canvas, center: Float, radius: Float, px: Float, selected: Boolean, dark: Boolean) {
        val fill = if (dark) CoffeePeekColors.DarkSurface else CoffeePeekColors.LightSurface
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill.toArgb()
            setShadowLayer(
                (if (selected) 8f else 3f) * px, 0f, (if (selected) 2f else 1f) * px,
                ColorUtils.setAlphaComponent(SHADOW, 0x59),
            )
        }
        canvas.drawCircle(center, center, radius, paint)
    }

    private fun drawPinIcon(context: Context, canvas: Canvas, resId: Int, center: Float, size: Float, dark: Boolean) {
        val icon = iconCache.getOrPut(resId) { requireNotNull(BitmapFactory.decodeResource(context.resources, resId)) }
        val tint = if (dark) CoffeePeekColors.DarkTextPrimary else CoffeePeekColors.DarkSurface
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tint.toArgb(), PorterDuff.Mode.SRC_IN)
        }
        val half = size * 0.30f
        canvas.drawBitmap(icon, null, RectF(center - half, center - half, center + half, center + half), paint)
    }

    private fun drawPinRing(canvas: Canvas, center: Float, radius: Float, px: Float, selected: Boolean, dark: Boolean) {
        val border = if (dark) CoffeePeekColors.DarkBorder else CoffeePeekColors.LightBorder
        val stroke = (if (selected) SELECTED_HALO_STROKE_DP else RING_DEFAULT_DP) * px
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = (if (selected) CoffeePeekColors.GoldWarm else border).toArgb()
        }
        val ringRadius = if (selected) radius + SELECTED_HALO_GAP_DP * px + stroke / 2f else radius - stroke / 2f
        canvas.drawCircle(center, center, ringRadius, paint)
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
}

@PreviewLightDark
@Composable
private fun MapMarkerIconsPreview() {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val background = if (dark) CoffeePeekColors.DarkBackground else CoffeePeekColors.LightBackground
    Column(Modifier.background(background)) {
        listOf(MapPinVisual.Default, MapPinVisual.Selected).forEach { visual ->
            Row {
                listOf(CoffeeShopType.SPECIALTY, CoffeeShopType.COFFEE_BAR, CoffeeShopType.CAFE).forEach { type ->
                    val bitmap = MapMarkerIcons.pinBitmap(context, type, visual, dark)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size((bitmap.width / context.resources.displayMetrics.density).dp),
                    )
                }
            }
        }
    }
}
