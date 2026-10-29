package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.media.PrefetchPhotos
import com.coffeepeek.core.media.image.FullScreenPhotoPresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekPhotoViewer(imageUrl: String, onDismiss: () -> Unit) =
    CoffeePeekPhotoViewer(listOf(imageUrl), 0, onDismiss)

@Composable
fun CoffeePeekPhotoViewer(imageUrls: List<String>, initialIndex: Int, onDismiss: () -> Unit) =
    CoffeePeekPhotoViewer(CoffeePeekPhotoViewerImages(imageUrls), initialIndex, onDismiss)

/** Paging, pinch/pan/double-tap, controls and swipe-down dismissal; originals wait for zoom. */
@Composable
fun CoffeePeekPhotoViewer(images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit) {
    PhotoGallery(images, initialIndex, onDismiss, prefetch = true) { frame, modifier ->
        CoffeePeekImage(frame.url, highQualityData = frame.detail, upgradeDelayMillis = 0,
            showLoader = true, contentDescription = frame.description, contentScale = ContentScale.Fit, modifier = modifier)
    }
}

/** Attachment previews reuse the same gestures while retaining their caller-supplied local renderer. */
@Composable
internal fun CoffeePeekPhotoViewer(count: Int, initialIndex: Int, onDismiss: () -> Unit, photo: @Composable (Int, Modifier) -> Unit) {
    val ids = remember(count) { List(count.coerceAtLeast(0)) { it.toString() } }
    PhotoGallery(CoffeePeekPhotoViewerImages(ids, ids), initialIndex, onDismiss, prefetch = false) { frame, modifier ->
        photo(frame.index, modifier)
    }
}

private data class ViewerFrame(val index: Int, val url: String, val detail: String?, val description: String)

@Composable
private fun PhotoGallery(
    images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit, prefetch: Boolean,
    photo: @Composable (ViewerFrame, Modifier) -> Unit,
) {
    if (images.previews.isEmpty()) return
    DisposableEffect(Unit) {
        FullScreenPhotoPresence.enter()
        onDispose { FullScreenPhotoPresence.leave() }
    }
    LocalCoffeePeekPhotoViewerPlatform.current.overlay()
    val pager = rememberPagerState(initialPage = initialIndex.coerceIn(0, images.previews.lastIndex), pageCount = { images.previews.size })
    if (prefetch) PrefetchPhotos(images.adjacentPreviewUrls(pager.currentPage), inPhotoViewer = true)
    CoffeePeekFullScreenDialog(onDismiss) { PhotoViewerContent(images, pager, onDismiss, photo) }
}

private class ViewerDismissState {
    var offset by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(1f)
    suspend fun finish(velocity: Float, distance: Float, fling: Float, reduced: Boolean, onDismiss: () -> Unit) {
        val dismiss = offset >= distance || velocity >= fling
        animate(offset, if (dismiss) height else 0f, animationSpec = tween(if (reduced) 0 else 180)) { value, _ -> offset = value }
        if (dismiss) onDismiss()
    }
}

@Composable
private fun PhotoViewerContent(
    images: CoffeePeekPhotoViewerImages, pager: PagerState, onDismiss: () -> Unit,
    photo: @Composable (ViewerFrame, Modifier) -> Unit,
) {
    var zoomed by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    val dismiss = remember { ViewerDismissState() }
    val reduced = LocalCoffeePeekPhotoViewerPlatform.current.reduceMotion()
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    val fling = with(LocalDensity.current) { 900.dp.toPx() }
    val drag = rememberDraggableState { dismiss.offset = (dismiss.offset + it).coerceAtLeast(0f) }
    PhotoViewerSurface(0.8f * (1f - dismiss.offset / dismiss.height).coerceIn(0f, 1f),
        Modifier.onSizeChanged { dismiss.height = it.height.toFloat().coerceAtLeast(1f) }
            .draggable(drag, Orientation.Vertical, enabled = !zoomed,
                onDragStopped = { dismiss.finish(it, threshold, fling, reduced, onDismiss) })) {
        PhotoViewerGallery(images, pager, zoomed, dismiss.offset, { controls = !controls }, { zoomed = it }, photo)
        if (controls) PhotoViewerControls(pager.currentPage, images.previews.size, onDismiss)
    }
}

@Composable
private fun PhotoViewerSurface(alpha: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha))
        .windowInsetsPadding(WindowInsets.safeDrawing).clipToBounds().then(modifier), content = content)
}

@Composable
private fun PhotoViewerGallery(
    images: CoffeePeekPhotoViewerImages, pager: PagerState, zoomed: Boolean, offset: Float,
    onTap: () -> Unit, onZoom: (Boolean) -> Unit, photo: @Composable (ViewerFrame, Modifier) -> Unit,
) {
    val visible by remember(pager) { derivedStateOf { pager.layoutInfo.visiblePagesInfo.map { it.index }.toSet() } }
    HorizontalPager(pager, beyondViewportPageCount = 0, userScrollEnabled = !zoomed,
        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = offset }) { page ->
        val current = pager.currentPage == page
        val url = images.displayUrl(page, pager.currentPage, page in visible)
        val detail = images.highQualityUrl(page, pager.settledPage).takeUnless { pager.isScrollInProgress }
        if (current || page in visible) ZoomablePhoto(ViewerFrame(page, url.orEmpty(), detail,
            stringResource(Res.string.coffeepeek_photo_description, page + 1, images.previews.size)),
            current, onTap, { if (current) onZoom(it) }, photo)
    }
}

@Composable
private fun BoxScope.PhotoViewerControls(page: Int, count: Int, onDismiss: () -> Unit) {
    if (count > 1) Text(stringResource(Res.string.coffeepeek_photo_position, page + 1, count),
        style = MaterialTheme.typography.labelLarge, color = Color.White,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = CoffeePeekDimens.spacing4)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1))
    CoffeePeekGlassIconButton(onClick = onDismiss, nativeIcon = CoffeePeekGlassIcon.Close,
        contentDescription = stringResource(Res.string.coffeepeek_viewer_close),
        modifier = Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing3)) {
        Icon(CoffeePeekIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

private class PhotoZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
    private fun clamp(value: Offset, zoom: Float): Offset {
        val x = size.width * (zoom - 1f) / 2f
        val y = size.height * (zoom - 1f) / 2f
        return Offset(value.x.coerceIn(-x, x), value.y.coerceIn(-y, y))
    }
    fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        val next = (scale * zoom).coerceIn(1f, 5f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val anchor = if (centroid.isSpecified) centroid - center else Offset.Zero
        offset = clamp((offset - anchor) * (next / scale) + anchor + pan, next)
        scale = next
    }
    suspend fun doubleTap(tap: Offset) {
        val start = scale
        val position = offset
        val target = if (scale > 1f) 1f else 2.5f
        val center = Offset(size.width / 2f, size.height / 2f)
        val end = clamp((center - tap) * (target - 1f), target)
        animate(0f, 1f, animationSpec = tween(250)) { t, _ ->
            scale = start + (target - start) * t
            offset = position + (end - position) * t
        }
    }
}

@Composable
private fun ZoomablePhoto(
    frame: ViewerFrame, current: Boolean, onTap: () -> Unit, onZoom: (Boolean) -> Unit,
    photo: @Composable (ViewerFrame, Modifier) -> Unit,
) {
    val zoom = remember { PhotoZoomState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(current) { if (!current) zoom.reset() }
    LaunchedEffect(zoom.scale > 1f) { onZoom(zoom.scale > 1f) }
    var zoomedOnce by remember(frame.url) { mutableStateOf(false) }
    LaunchedEffect(zoom.scale > 1f) { if (zoom.scale > 1f) zoomedOnce = true }
    val selected = frame.copy(detail = frame.detail?.takeIf { zoomedOnce && it != frame.url })
    photo(selected, Modifier.fillMaxSize().onSizeChanged { zoom.size = it }.photoTapGestures(zoom, scope, onTap)
        .photoPanGestures(zoom).graphicsLayer {
            scaleX = zoom.scale
            scaleY = zoom.scale
            translationX = zoom.offset.x
            translationY = zoom.offset.y
        })
}

private fun Modifier.photoTapGestures(state: PhotoZoomState, scope: CoroutineScope, onTap: () -> Unit) = pointerInput(Unit) {
    detectTapGestures(onTap = { onTap() }, onDoubleTap = { tap -> scope.launch { state.doubleTap(tap) } })
}

private fun Modifier.photoPanGestures(state: PhotoZoomState) = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pinching = event.changes.count { it.pressed } > 1
            if (pinching || state.scale > 1f) {
                state.transform(event.calculateZoom(), event.calculatePan(), event.calculateCentroid(useCurrent = true))
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (state.scale <= 1f) state.offset = Offset.Zero
    }
}

@Preview @Composable private fun CoffeePeekPhotoViewerLightPreview() = PhotoViewerPreview(false)
@Preview @Composable private fun CoffeePeekPhotoViewerDarkPreview() = PhotoViewerPreview(true)

@Composable
private fun PhotoViewerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PhotoViewerSurface(0.8f) {
        Image(ColorPainter(MaterialTheme.colorScheme.primary), null,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(240.dp))
        PhotoViewerControls(0, 3, {})
    }
}
