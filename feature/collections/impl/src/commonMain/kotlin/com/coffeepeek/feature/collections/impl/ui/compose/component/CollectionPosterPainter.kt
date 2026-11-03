package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlin.math.roundToInt

/** One reference-based template for both preview and PNG, with separate shop and roaster cards. */
internal class CollectionPosterPainter(
    private val collection: SharedCollection,
    private val format: CollectionPosterFormat,
    private val page: Int,
    private val resources: CollectionPosterResources,
    private val qr: Painter,
    private val photos: Map<String, ImageBitmap> = emptyMap(),
) : Painter() {
    override val intrinsicSize = Size(format.width.toFloat(), format.height.toFloat())
    private val gold = Color(0xFFEAB308)
    private val muted = Color(0xFFCDC2B7)
    private val ink = Color(0xFFF8F5F1)

    override fun DrawScope.onDraw() {
        withTransform({ scale(size.width / format.width, size.height / format.height, Offset.Zero) }) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF352B25), Color(0xFF191614)),
                Offset(240f, 360f), format.height * 0.8f), size = intrinsicSize)
            heading()
            val places = collectionPosterPage(collection.places, format, page)
            collectionPosterCards(places, format).forEach { card ->
                if (card.place.kind == CollectionPlaceKind.CoffeeShop) shopCard(card) else roasterCard(card)
            }
            footer()
        }
    }

    private fun DrawScope.heading() {
        text("COFFEEPEEK", 56f, 50f, 968f, 28, gold, FontWeight.SemiBold)
        text(collection.title, 56f, 100f, 968f, 44, ink, FontWeight.Bold, 2)
        text(resources.labels.count, 56f, 208f, 968f, 24, muted)
    }

    private fun DrawScope.panel(top: Float, height: Float) {
        val offset = Offset(32f, top)
        val panelSize = Size(1016f, height)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF3D322A), Color(0xFF29221E))),
            offset, panelSize, CornerRadius(28f))
        drawRoundRect(Color(0xFF685748).copy(alpha = 0.28f), offset, panelSize, CornerRadius(28f), style = Stroke(1f))
    }

    private fun DrawScope.shopCard(card: CollectionPosterCard) {
        val place = card.place
        val y = card.top
        panel(y, card.height)
        photo(place, Rect(Offset(46f, y + 14f), Size(316f, card.height - 28f)), fit = false)
        text(place.title, 390f, y + 24f, 420f, 40, ink, FontWeight.Bold, 2)
        kindChip(resources.labels.shop, resources.icons.shop, 390f, y + 120f)
        shopStatistics(place, y)
        location(place, 390f, y + card.height - 43f, 592f)
        icon(resources.icons.chevron, 1004f, y + card.height / 2f - 16f, 30f, muted)
    }

    private fun DrawScope.roasterCard(card: CollectionPosterCard) {
        val place = card.place
        val y = card.top
        panel(y, card.height)
        val side = card.height - 24f
        photo(place, Rect(Offset(46f, y + 12f), Size(side, side)), fit = true)
        val x = 46f + side + 28f
        text(place.title, x, y + 20f, 982f - x, 36, ink, FontWeight.Bold)
        kindChip(resources.labels.roaster, resources.icons.roaster, x, y + 78f)
        val address = listOfNotNull(place.city, place.address).filter(String::isNotBlank).joinToString(" · ")
        if (address.isNotEmpty()) text(address, x, y + 126f, 982f - x, 22, muted)
        icon(resources.icons.chevron, 1004f, y + card.height / 2f - 16f, 30f, muted)
    }

    private fun DrawScope.kindChip(label: String, painter: Painter, x: Float, y: Float) {
        val width = resources.measurer.measure(AnnotatedString(label),
            resources.typography.copy(fontSize = 22.sp), density = Density(1f)).size.width + 70f
        drawRoundRect(Color(0xFF62503E).copy(alpha = 0.72f), Offset(x, y), Size(width, 40f), CornerRadius(20f))
        icon(painter, x + 14f, y + 7f, 26f, Color(0xFFE4CFAE))
        text(label, x + 48f, y + 7f, width - 58f, 22, ink)
    }

    private fun DrawScope.shopStatistics(place: CollectionPlace, y: Float) {
        place.rating?.takeIf { it.isFinite() && it > 0.0 && it <= 5.0 }?.let { rating ->
            drawRoundRect(Color(0xFF514438), Offset(834f, y + 28f), Size(146f, 56f), CornerRadius(28f))
            drawRoundRect(Color(0xFF78634E).copy(alpha = 0.55f), Offset(834f, y + 28f), Size(146f, 56f), CornerRadius(28f), style = Stroke(1f))
            icon(resources.icons.star, 850f, y + 40f, 32f, gold)
            val tenths = (rating * 10).roundToInt()
            text("${tenths / 10}.${tenths % 10}", 894f, y + 36f, 76f, 34, ink, FontWeight.Bold)
        }
        resources.labels.checkIns[place.key]?.let { text("($it)", 824f, y + 96f, 178f, 21, muted) }
    }

    private fun DrawScope.location(place: CollectionPlace, x: Float, y: Float, width: Float) {
        val address = listOfNotNull(place.city, place.address).filter(String::isNotBlank).joinToString(" · ")
        if (address.isEmpty()) return
        icon(resources.icons.location, x, y, 26f, gold)
        text(address, x + 36f, y, width - 36f, 24, muted)
    }

    private fun DrawScope.photo(place: CollectionPlace, rect: Rect, fit: Boolean) {
        val clip = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(22f))) }
        clipPath(clip) {
            drawRect(if (fit) Color.White else Color(0xFF493A2D), rect.topLeft, rect.size)
            val image = photos[place.key]
            if (image != null) drawPhotoImage(image, rect, fit)
            else icon(if (fit) resources.icons.roaster else resources.icons.shop,
                rect.center.x - 26f, rect.center.y - 26f, 52f, if (fit) Color(0xFF79634D) else gold)
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
        val y = format.height - 142f
        drawRoundRect(Color.White, Offset(56f, y), Size(128f, 128f), CornerRadius(14f))
        withTransform({ translate(68f, y + 12f) }) { with(qr) { draw(Size(104f, 104f)) } }
        text(resources.labels.qrHint, 208f, y + 14f, 500f, 24, muted, lines = 2)
        if (resources.labels.pages.size > 1) text(resources.labels.pages.getOrElse(page) { "" }, 208f, y + 88f, 400f, 22, muted)
        text("coffeepeek.by", 758f, y + 82f, 272f, 30, gold)
    }

    private fun DrawScope.icon(painter: Painter, x: Float, y: Float, side: Float, tint: Color) {
        withTransform({ translate(x, y) }) { with(painter) { draw(Size(side, side), colorFilter = ColorFilter.tint(tint)) } }
    }

    private fun DrawScope.text(value: String, x: Float, y: Float, width: Float, fontSize: Int, color: Color,
        weight: FontWeight = FontWeight.Normal, lines: Int = 1) {
        val layout = resources.measurer.measure(AnnotatedString(value), style = resources.typography.copy(fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.12f).sp, color = color, fontWeight = weight, letterSpacing = 0.sp), maxLines = lines,
            overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = width.toInt()), density = Density(1f))
        drawText(layout, topLeft = Offset(x, y))
    }
}
