package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.BagCropBounds
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs

/** Lets the user select an upright rectangular crop of the original photo without scanner models. */
@Composable
internal fun ShelfManualCrop(
    photo: ByteArray, initialBounds: BagCropBounds, cropping: Boolean, onApply: (BagCropBounds) -> Unit,
    onBack: () -> Unit, modifier: Modifier = Modifier,
) {
    var bounds by remember(photo) { mutableStateOf(initialBounds) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.shelf_manual_crop_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.shelf_manual_crop_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val cropLabel = stringResource(Res.string.shelf_manual_crop_accessibility)
        ShelfPhotoBox(photo, Modifier.fillMaxWidth().height(300.dp).semantics { contentDescription = cropLabel }) {
            ShelfCropOverlay(bounds) { bounds = it }
        }
        ShelfCropControls(bounds) { bounds = it }
        if (cropping) CoffeePeekLoader(stringResource(Res.string.shelf_manual_crop_working))
        CoffeePeekButton(stringResource(Res.string.shelf_manual_crop_confirm), { onApply(bounds) }, enabled = !cropping)
        ShelfSecondaryButton(stringResource(Res.string.shelf_manual_crop_back), onBack, enabled = !cropping)
    }
}

@Composable
private fun ShelfCropControls(bounds: BagCropBounds, onBounds: (BagCropBounds) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        ShelfCropEdgeSlider(stringResource(Res.string.shelf_crop_left), bounds.left, 0f..(bounds.right - 0.1f)) {
            onBounds(bounds.copy(left = it))
        }
        ShelfCropEdgeSlider(stringResource(Res.string.shelf_crop_right), bounds.right, (bounds.left + 0.1f)..1f) {
            onBounds(bounds.copy(right = it))
        }
        ShelfCropEdgeSlider(stringResource(Res.string.shelf_crop_top), bounds.top, 0f..(bounds.bottom - 0.1f)) {
            onBounds(bounds.copy(top = it))
        }
        ShelfCropEdgeSlider(stringResource(Res.string.shelf_crop_bottom), bounds.bottom, (bounds.top + 0.1f)..1f) {
            onBounds(bounds.copy(bottom = it))
        }
    }
}

@Composable
private fun ShelfCropEdgeSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(value, onValue, valueRange = range, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
    }
}

@Composable
private fun ShelfCropOverlay(bounds: BagCropBounds, onBounds: (BagCropBounds) -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    val latestBounds by rememberUpdatedState(bounds)
    val latestChange by rememberUpdatedState(onBounds)
    Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
        var working = latestBounds
        var handle = CropHandle.Move
        detectDragGestures(onDragStart = { start ->
            working = latestBounds
            handle = cropHandle(working, start.x / size.width, start.y / size.height)
        }) { change, drag ->
            change.consume()
            working = moveCrop(working, handle, drag.x / size.width, drag.y / size.height)
            latestChange(working)
        }
    }) { drawCrop(bounds, color) }
}

private enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight, Move }

private fun cropHandle(bounds: BagCropBounds, x: Float, y: Float): CropHandle {
    val corners = listOf(
        CropHandle.TopLeft to (abs(x - bounds.left) + abs(y - bounds.top)),
        CropHandle.TopRight to (abs(x - bounds.right) + abs(y - bounds.top)),
        CropHandle.BottomLeft to (abs(x - bounds.left) + abs(y - bounds.bottom)),
        CropHandle.BottomRight to (abs(x - bounds.right) + abs(y - bounds.bottom)),
    )
    return corners.minBy { it.second }.takeIf { it.second < 0.2f }?.first ?: CropHandle.Move
}

private fun moveCrop(bounds: BagCropBounds, handle: CropHandle, dx: Float, dy: Float): BagCropBounds = when (handle) {
    CropHandle.TopLeft -> bounds.copy(left = (bounds.left + dx).coerceIn(0f, bounds.right - 0.1f),
        top = (bounds.top + dy).coerceIn(0f, bounds.bottom - 0.1f))
    CropHandle.TopRight -> bounds.copy(right = (bounds.right + dx).coerceIn(bounds.left + 0.1f, 1f),
        top = (bounds.top + dy).coerceIn(0f, bounds.bottom - 0.1f))
    CropHandle.BottomLeft -> bounds.copy(left = (bounds.left + dx).coerceIn(0f, bounds.right - 0.1f),
        bottom = (bounds.bottom + dy).coerceIn(bounds.top + 0.1f, 1f))
    CropHandle.BottomRight -> bounds.copy(right = (bounds.right + dx).coerceIn(bounds.left + 0.1f, 1f),
        bottom = (bounds.bottom + dy).coerceIn(bounds.top + 0.1f, 1f))
    CropHandle.Move -> translateCrop(bounds, dx, dy)
}

private fun translateCrop(bounds: BagCropBounds, dx: Float, dy: Float): BagCropBounds {
    val x = dx.coerceIn(-bounds.left, 1f - bounds.right)
    val y = dy.coerceIn(-bounds.top, 1f - bounds.bottom)
    return BagCropBounds(bounds.left + x, bounds.top + y, bounds.right + x, bounds.bottom + y)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrop(bounds: BagCropBounds, color: Color) {
    val left = bounds.left * size.width
    val top = bounds.top * size.height
    val right = bounds.right * size.width
    val bottom = bounds.bottom * size.height
    val shade = Color.Black.copy(alpha = 0.48f)
    drawRect(shade, size = Size(size.width, top))
    drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
    drawRect(shade, Offset(0f, top), Size(left, bottom - top))
    drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
    drawRect(color, Offset(left, top), Size(right - left, bottom - top), style = Stroke(2.dp.toPx()))
    listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach {
        drawCircle(color, radius = 7.dp.toPx(), center = it)
    }
}

@Composable
private fun ShelfManualCropPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfManualCrop(byteArrayOf(), BagCropBounds.Default, false, {}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfManualCropLightPreview() = ShelfManualCropPreview(false)
@Preview @Composable private fun ShelfManualCropDarkPreview() = ShelfManualCropPreview(true)

@Composable
private fun ShelfCropOverlayPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Canvas(Modifier.fillMaxSize()) {
            val bag = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.12f)
                lineTo(size.width * 0.76f, size.height * 0.12f)
                lineTo(size.width * 0.8f, size.height * 0.9f)
                lineTo(size.width * 0.2f, size.height * 0.9f)
                close()
            }
            drawPath(bag, Color(0xFF815C42))
            drawCircle(Color(0xFFF4C54E), size.width * 0.1f, Offset(size.width * 0.5f, size.height * 0.5f))
        }
        ShelfCropOverlay(BagCropBounds.Default) {}
    }
}

@Preview @Composable private fun ShelfCropOverlayLightPreview() = ShelfCropOverlayPreview(false)
@Preview @Composable private fun ShelfCropOverlayDarkPreview() = ShelfCropOverlayPreview(true)
