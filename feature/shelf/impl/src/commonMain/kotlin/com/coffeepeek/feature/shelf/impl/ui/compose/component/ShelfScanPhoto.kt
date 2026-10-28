package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfPhotoShape = RoundedCornerShape(18.dp)

/**
 * An encoded image (the picked photo or the cut card), as large as fits in [modifier] at its own proportions and centred,
 * so [shape] and [elevation] follow the picture itself. A [frame] outlines the front the scanner found.
 */
@Composable
internal fun ShelfScanPhoto(
    bytes: ByteArray, frame: BagScanFrame?, modifier: Modifier = Modifier,
    shape: Shape = ShelfPhotoShape, elevation: Dp = 0.dp,
) {
    val bitmap by produceState<ImageBitmap?>(null, bytes) {
        value = withContext(Dispatchers.Default) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) {
            val room = if (constraints.hasBoundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(0.75f)
            Box(room.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
            return@BoxWithConstraints
        }
        val aspect = image.width.toFloat() / image.height
        val tallerThanRoom = constraints.hasBoundedHeight && maxWidth / maxHeight > aspect
        Box(Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = tallerThanRoom).shadow(elevation, shape).clip(shape)) {
            Image(image, stringResource(Res.string.shelf_scan_photo), Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            if (frame != null) ShelfFrameOutline(frame)
        }
    }
}

@Composable
private fun ShelfFrameOutline(frame: BagScanFrame) {
    val outline = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val path = Path()
        frame.points.forEachIndexed { index, (x, y) ->
            val point = Offset(x * size.width, y * size.height)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, outline, style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
private fun ShelfScanPhotoPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanPhoto(byteArrayOf(), null, Modifier.fillMaxWidth().height(240.dp).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfScanPhotoLightPreview() = ShelfScanPhotoPreview(false)
@Preview @Composable private fun ShelfScanPhotoDarkPreview() = ShelfScanPhotoPreview(true)
