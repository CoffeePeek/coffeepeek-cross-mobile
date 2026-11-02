package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat

internal data class CollectionPosterLabels(val shop: String, val roaster: String, val qrHint: String,
    val count: String, val pages: List<String>)

/** The same vector template draws the preview and the exported PNG; no screen capture or private UI. */
internal class CollectionPosterPainter(
    private val collection: SharedCollection,
    private val format: CollectionPosterFormat,
    private val page: Int,
    private val labels: CollectionPosterLabels,
    private val qr: Painter,
    private val measurer: TextMeasurer,
    private val typography: TextStyle,
    private val photos: Map<String, ImageBitmap> = emptyMap(),
) : Painter() {
    override val intrinsicSize = Size(format.width.toFloat(), format.height.toFloat())

    override fun DrawScope.onDraw() {
        withTransform({ scale(size.width / format.width, size.height / format.height, Offset.Zero) }) {
            drawRect(Color(0xFF1A1412), size = intrinsicSize)
            heading()
            val rowHeight = (format.height - 600f) / format.perPage
            collection.places.chunked(format.perPage).getOrElse(page) { emptyList() }.forEachIndexed { index, place ->
                placeRow(place, 320f + index * rowHeight, rowHeight)
            }
            footer()
        }
    }

    private fun DrawScope.heading() {
        text("COFFEEPEEK", 64f, 58f, 900f, 28, Color(0xFFEAB308), FontWeight.Bold)
        text(collection.title, 64f, 106f, 952f, 44, Color.White, FontWeight.Bold, 3)
        text(labels.count, 64f, 260f, 952f, 24, Color(0xFFB8AEA5))
    }

    private fun DrawScope.placeRow(place: CollectionPlace, y: Float, height: Float) {
        val photoSize = minOf(144f, height - 24f)
        val photoY = y + (height - 12f - photoSize) / 2f
        drawRoundRect(Color(0xFF2D241F), Offset(48f, y), Size(984f, height - 12f), CornerRadius(24f))
        val photo = photos[place.key]
        if (photo != null) placePhoto(photo, place.kind, photoY, photoSize)
        else text("COFFEEPEEK", 64f, photoY + photoSize / 2f - 7f, photoSize, 12, Color(0xFFEAB308), FontWeight.Bold)
        val x = 64f + photoSize + 22f
        text(place.title, x, y + 12f, 1000f - x, 42, Color.White, FontWeight.SemiBold, 2)
        val detail = listOfNotNull(place.city, place.address).filter(String::isNotBlank).joinToString(" · ")
            .ifBlank { if (place.kind == CollectionPlaceKind.CoffeeShop) labels.shop else labels.roaster }
        text(detail, x, y + height - 48f, 1000f - x, 32, Color(0xFFB8AEA5))
    }

    private fun DrawScope.placePhoto(photo: ImageBitmap, kind: CollectionPlaceKind, y: Float, side: Float) {
        if (kind == CollectionPlaceKind.Roaster) {
            drawRoundRect(Color.White, Offset(64f, y), Size(side, side), CornerRadius(12f))
            val ratio = minOf(side / photo.width, side / photo.height)
            val width = (photo.width * ratio).toInt().coerceAtLeast(1)
            val height = (photo.height * ratio).toInt().coerceAtLeast(1)
            drawImage(photo, dstOffset = IntOffset((64f + (side - width) / 2f).toInt(), (y + (side - height) / 2f).toInt()),
                dstSize = IntSize(width, height))
        } else {
            val sourceSide = minOf(photo.width, photo.height)
            drawImage(photo, srcOffset = IntOffset((photo.width - sourceSide) / 2, (photo.height - sourceSide) / 2),
                srcSize = IntSize(sourceSide, sourceSide), dstOffset = IntOffset(64, y.toInt()), dstSize = IntSize(side.toInt(), side.toInt()))
        }
    }

    private fun DrawScope.footer() {
        val y = format.height - 230f
        drawRoundRect(Color.White, Offset(64f, y), Size(192f, 192f), CornerRadius(16f))
        withTransform({ translate(80f, y + 16f) }) { with(qr) { draw(Size(160f, 160f)) } }
        text(labels.qrHint, 288f, y + 20f, 710f, 30, Color.White, FontWeight.SemiBold, 2)
        text("coffeepeek.by", 288f, y + 112f, 710f, 28, Color(0xFFEAB308))
        text(labels.pages.getOrElse(page) { "${page + 1}/${labels.pages.size}" }, 288f, y + 154f, 710f, 23, Color(0xFFB8AEA5))
    }

    private fun DrawScope.text(value: String, x: Float, y: Float, width: Float, fontSize: Int, color: Color,
        weight: FontWeight = FontWeight.Normal, lines: Int = 1) {
        val layout = measurer.measure(AnnotatedString(value), style = typography.copy(fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.12f).sp, color = color, fontWeight = weight), maxLines = lines,
            overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = width.toInt()), density = Density(1f))
        drawText(layout, topLeft = Offset(x, y))
    }
}
