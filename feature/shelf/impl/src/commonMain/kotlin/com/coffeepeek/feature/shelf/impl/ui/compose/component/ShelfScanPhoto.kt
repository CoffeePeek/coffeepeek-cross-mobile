package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.BagScanFrame
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_scan_photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfPhotoShape = RoundedCornerShape(18.dp)

/** Decodes [bytes] off the main thread; null until done or when they are not an image. */
@Composable
internal fun rememberShelfBitmap(bytes: ByteArray): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, bytes) {
        value = withContext(Dispatchers.Default) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    }
    return bitmap
}

/**
 * An encoded image (the picked photo or the cut card), as large as fits in [modifier] at its own proportions and centred,
 * so [shape] and [elevation] follow the picture itself. A [frame] outlines the front the scanner found in [frameColor].
 */
@Composable
internal fun ShelfScanPhoto(
    bytes: ByteArray, frame: BagScanFrame?, modifier: Modifier = Modifier,
    shape: Shape = ShelfPhotoShape, elevation: Dp = 0.dp, frameColor: Color = MaterialTheme.colorScheme.primary,
) {
    ShelfPhotoBox(bytes, modifier, shape, elevation) { if (frame != null) ShelfFrameOutline(frame, frameColor) }
}

/** The picture at its own proportions, with [overlay] drawn in exactly its bounds; [onBounds] gets them in root coordinates. */
@Composable
internal fun ShelfPhotoBox(
    bytes: ByteArray, modifier: Modifier = Modifier, shape: Shape = ShelfPhotoShape, elevation: Dp = 0.dp,
    onBounds: (Rect) -> Unit = {}, overlay: @Composable BoxScope.() -> Unit = {},
) {
    val image = rememberShelfBitmap(bytes)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        if (image == null) {
            val room = if (constraints.hasBoundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(0.75f)
            Box(room.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
            return@BoxWithConstraints
        }
        val aspect = image.width.toFloat() / image.height
        val tallerThanRoom = constraints.hasBoundedHeight && maxWidth / maxHeight > aspect
        Box(Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = tallerThanRoom).onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .shadow(elevation, shape).clip(shape)) {
            Image(image, stringResource(Res.string.shelf_scan_photo), Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            overlay()
        }
    }
}

@Composable
private fun ShelfFrameOutline(frame: BagScanFrame, color: Color) {
    Canvas(Modifier.fillMaxSize()) { drawPath(frame.path(size), color, style = Stroke(3.dp.toPx(), join = StrokeJoin.Round)) }
}

/** The frame's quad in a drawing of [size]. */
internal fun BagScanFrame.path(size: Size): Path = Path().apply {
    points.forEachIndexed { index, (x, y) -> if (index == 0) moveTo(x * size.width, y * size.height) else lineTo(x * size.width, y * size.height) }
    close()
}

@Composable
private fun ShelfScanPhotoPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanPhoto(byteArrayOf(), null, Modifier.fillMaxWidth().height(240.dp).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfScanPhotoLightPreview() = ShelfScanPhotoPreview(false)
@Preview @Composable private fun ShelfScanPhotoDarkPreview() = ShelfScanPhotoPreview(true)
