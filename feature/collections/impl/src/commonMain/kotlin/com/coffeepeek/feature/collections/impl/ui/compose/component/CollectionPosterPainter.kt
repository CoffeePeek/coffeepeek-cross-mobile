package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlin.math.roundToInt

/** The measured layout is shared by pagination, accessible preview and exported PNGs. */
internal class CollectionPosterPainter(
    private val collection: SharedCollection,
    private val format: CollectionPosterFormat,
    private val page: Int,
    private val resources: CollectionPosterResources,
    private val photos: Map<String, ImageBitmap> = emptyMap(),
) : Painter() {
    override val intrinsicSize = Size(format.width.toFloat(), format.height.toFloat())
    private val gold = Color(0xFFEAB308)
    private val muted = Color(0xFFCDC2B7)
    private val ink = Color(0xFFF8F5F1)
    private val detailFontSize = 22

    override fun DrawScope.onDraw() {
        withTransform({ scale(size.width / format.width, size.height / format.height, Offset.Zero) }) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF352B25), Color(0xFF191614)),
                Offset(240f, 360f), format.height * 0.8f), size = intrinsicSize)
            heading()
            resources.layout.pages.getOrElse(page) { emptyList() }.forEach { card ->
                panel(card)
                if (card.place.kind == CollectionPlaceKind.CoffeeShop) shopCard(card) else roasterCard(card)
            }
            footer()
        }
    }

    private fun DrawScope.heading() {
        brandStrip(0f)
        val title = text(collection.title, 32f, 76f, 1016f, 44, ink, FontWeight.Bold, 2)
        text(resources.labels.count, 32f, 84f + title, 1016f, 24, muted)
    }

    private fun DrawScope.brandStrip(y: Float) {
        drawRect(Color(0xFF171411), Offset(0f, y), Size(1080f, 44f))
        val width = measure(resources.labels.brand, 300f, 22, FontWeight.SemiBold).size.width.toFloat()
        var x = -36f
        while (x < format.width) {
            text(resources.labels.brand, x, y + 9f, width + 2f, 22, gold, FontWeight.SemiBold)
            x += width + 24f
        }
    }

    private fun DrawScope.panel(card: CollectionPosterCard) {
        val offset = Offset(card.left, card.top)
        val panelSize = Size(card.width, card.height)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF3D322A), Color(0xFF29221E))),
            offset, panelSize, CornerRadius(26f))
        drawRoundRect(Color(0xFF685748).copy(alpha = 0.28f), offset, panelSize, CornerRadius(26f), style = Stroke(1f))
    }

    private fun DrawScope.shopCard(card: CollectionPosterCard) {
        photo(card.place, Rect(Offset(card.left + 12f, card.top + 12f), Size(296f, card.height - 24f)), false)
        val statistics = shopStatisticsWidth(card.place)
        val width = card.width - 390f - if (statistics > 0f) statistics + 16f else 0f
        placeDetails(card, card.left + 332f, width, 38, true)
        shopStatistics(card)
        icon(resources.icons.chevron, card.left + card.width - 44f, card.top + card.height / 2f - 14f, 28f, muted)
    }

    private fun DrawScope.roasterCard(card: CollectionPosterCard) {
        photo(card.place, Rect(Offset(card.left + 12f, card.top + (card.height - 128f) / 2), Size(128f, 128f)), true)
        placeDetails(card, card.left + 156f, card.width - 196f, 30, false)
        icon(resources.icons.chevron, card.left + card.width - 34f, card.top + card.height / 2f - 12f, 24f, muted)
    }

    private fun DrawScope.placeDetails(card: CollectionPosterCard, x: Float, width: Float,
        titleSize: Int, shop: Boolean) {
        val title = measure(card.place.title, width, titleSize, FontWeight.Bold, 2)
        val address = listOfNotNull(card.place.city, card.place.address).filter(String::isNotBlank).joinToString(" · ")
        val addressWidth = width - if (shop) 34f else 0f
        val location = address.takeIf(String::isNotEmpty)?.let { measure(it, addressWidth, detailFontSize, lines = 2) }
        val height = title.size.height + if (location != null) 8f + maxOf(location.size.height.toFloat(), 24f) else 0f
        val y = card.top + (card.height - height) / 2
        drawText(title, ink, Offset(x, y))
        location?.let { drawLocation(it, x, y + title.size.height + 8f, shop) }
    }

    private fun DrawScope.drawLocation(layout: TextLayoutResult, x: Float, y: Float, shop: Boolean) {
        if (shop) icon(resources.icons.location, x, y, 24f, gold)
        drawText(layout, muted, Offset(x + if (shop) 34f else 0f, y))
    }

    private fun shopStatisticsWidth(place: CollectionPlace): Float {
        val count = resources.labels.checkIns[place.key]?.let { measure(it, 300f, detailFontSize).size.width.toFloat() } ?: 0f
        val rating = if (place.rating?.let { it.isFinite() && it > 0.0 && it <= 5.0 } == true) 146f else 0f
        return maxOf(count, rating)
    }

    private fun DrawScope.shopStatistics(card: CollectionPosterCard) {
        val rating = card.place.rating?.takeIf { it.isFinite() && it > 0.0 && it <= 5.0 }
        val checkIns = resources.labels.checkIns[card.place.key]
        val width = shopStatisticsWidth(card.place)
        val countHeight = checkIns?.let { measure(it, width, detailFontSize).size.height.toFloat() } ?: 0f
        val height = (if (rating != null) 56f else 0f) + countHeight + if (rating != null && checkIns != null) 6f else 0f
        val y = card.top + (card.height - height) / 2
        val right = card.left + card.width - 58f
        if (rating != null) ratingBadge(rating, right - 146f, y)
        checkIns?.let {
            text(it, right - width, y + if (rating != null) 62f else 0f, width, detailFontSize, muted, align = TextAlign.End)
        }
    }

    private fun DrawScope.ratingBadge(value: Double, x: Float, y: Float) {
        drawRoundRect(Color(0xFF514438), Offset(x, y), Size(146f, 56f), CornerRadius(28f))
        drawRoundRect(Color(0xFF78634E).copy(alpha = 0.55f), Offset(x, y), Size(146f, 56f), CornerRadius(28f), style = Stroke(1f))
        icon(resources.icons.star, x + 16f, y + 12f, 32f, gold)
        val tenths = (value * 10).roundToInt()
        text("${tenths / 10}.${tenths % 10}", x + 60f, y + 8f, 76f, 34, ink, FontWeight.Bold)
    }

    private fun DrawScope.photo(place: CollectionPlace, rect: Rect, fit: Boolean) {
        val clip = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(20f))) }
        clipPath(clip) {
            drawRect(if (fit) Color.White else Color(0xFF493A2D), rect.topLeft, rect.size)
            val image = photos[place.key]
            if (image != null) drawPhotoImage(image, rect, fit)
            else withTransform({ translate(rect.left, rect.top) }) {
                with(resources.icons.placeholder) { draw(rect.size) }
            }
        }
    }

    private fun DrawScope.drawPhotoImage(image: ImageBitmap, rect: Rect, fit: Boolean) {
        if (fit) {
            val scale = minOf((rect.width - 20f) / image.width, (rect.height - 20f) / image.height)
            val width = (image.width * scale).roundToInt().coerceAtLeast(1)
            val height = (image.height * scale).roundToInt().coerceAtLeast(1)
            drawImage(image, dstOffset = IntOffset((rect.center.x - width / 2f).roundToInt(), (rect.center.y - height / 2f).roundToInt()),
                dstSize = IntSize(width, height))
        } else {
            val scale = maxOf(rect.width / image.width, rect.height / image.height)
            val width = (rect.width / scale).roundToInt().coerceIn(1, image.width)
            val height = (rect.height / scale).roundToInt().coerceIn(1, image.height)
            drawImage(image, srcOffset = IntOffset((image.width - width) / 2, (image.height - height) / 2), srcSize = IntSize(width, height),
                dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()), dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()))
        }
    }

    private fun DrawScope.footer() {
        val y = format.height - 82f
        if (resources.labels.pages.size > 1) text(resources.labels.pages.getOrElse(page) { "" }, 32f, y, 300f, 22, muted)
        brandStrip(format.height - 44f)
    }

    private fun DrawScope.icon(painter: Painter, x: Float, y: Float, side: Float, tint: Color) {
        withTransform({ translate(x, y) }) { with(painter) { draw(Size(side, side), colorFilter = ColorFilter.tint(tint)) } }
    }

    private fun measure(value: String, width: Float, size: Int, weight: FontWeight = FontWeight.Normal,
        lines: Int = 1, align: TextAlign = TextAlign.Start): TextLayoutResult =
        measureCollectionPosterText(resources.measurer, resources.typography, value, width, size, weight, lines, align)

    private fun DrawScope.text(value: String, x: Float, y: Float, width: Float, fontSize: Int, color: Color,
        weight: FontWeight = FontWeight.Normal, lines: Int = 1, align: TextAlign = TextAlign.Start): Float {
        val layout = measure(value, width, fontSize, weight, lines, align)
        drawText(layout, color, Offset(x, y))
        return layout.size.height.toFloat()
    }
}
