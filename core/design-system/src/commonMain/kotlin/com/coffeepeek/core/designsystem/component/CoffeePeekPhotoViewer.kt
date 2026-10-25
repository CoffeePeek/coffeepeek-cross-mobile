package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

/** Image content must fit the viewport; paging and zooming never modify the attachments. */
@Composable
internal fun CoffeePeekPhotoViewer(
    count: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    photo: @Composable (index: Int, Modifier) -> Unit,
) {
    if (count <= 0) return
    val pager = rememberPagerState(initialPage = initialIndex.coerceIn(0, count - 1), pageCount = { count })
    var zoomed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PhotoViewerSurface(pager.currentPage, count, onDismiss) {
            HorizontalPager(pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
                PhotoViewerPage(page, count, page == pager.currentPage, { zoomed = it }) { photo(page, it) }
            }
        }
    }
}

@Composable
private fun PhotoViewerSurface(page: Int, count: Int, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        content()
        Text(stringResource(Res.string.coffeepeek_photo_position, page + 1, count),
            style = MaterialTheme.typography.labelLarge, color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
            Icon(CoffeePeekIcons.Close, stringResource(Res.string.coffeepeek_photo_close), tint = Color.White)
        }
    }
}

@Composable
private fun PhotoViewerPage(
    index: Int, count: Int, current: Boolean, onZoom: (Boolean) -> Unit,
    photo: @Composable (Modifier) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { centroid, zoom, pan, _ ->
        val next = (scale * zoom).coerceIn(1f, 5f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val anchor = if (centroid.isSpecified) centroid - center else Offset.Zero
        offset = clampPhotoOffset((offset - anchor) * (next / scale) + anchor + pan, next, size)
        scale = next
    }
    LaunchedEffect(current) {
        if (!current) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    LaunchedEffect(current, scale > 1f) { if (current) onZoom(scale > 1f) }
    val description = stringResource(Res.string.coffeepeek_photo_description, index + 1, count)
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { size = it }
        .transformable(transform, canPan = { scale > 1f })
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { tap ->
                scale = if (scale > 1f) 1f else 2.5f
                val center = Offset(size.width / 2f, size.height / 2f)
                offset = clampPhotoOffset((center - tap) * (scale - 1f), scale, size)
            })
        }
        .semantics { contentDescription = description }) {
        photo(Modifier.fillMaxSize().graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        })
    }
}

private fun clampPhotoOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    val maxX = size.width * (scale - 1f) / 2f
    val maxY = size.height * (scale - 1f) / 2f
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

@Preview @Composable private fun CoffeePeekPhotoViewerLightPreview() = PhotoViewerPreview(false)
@Preview @Composable private fun CoffeePeekPhotoViewerDarkPreview() = PhotoViewerPreview(true)

@Composable
private fun PhotoViewerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PhotoViewerSurface(0, 3, {}) { CoffeePeekImagePlaceholder(Modifier.fillMaxSize()) }
}
