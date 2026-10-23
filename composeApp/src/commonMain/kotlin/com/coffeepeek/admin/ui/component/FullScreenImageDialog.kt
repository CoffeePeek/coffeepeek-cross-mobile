package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.ColorPainter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PrefetchPhotos
import com.coffeepeek.admin.utils.image.FullScreenPhotoPresence
import com.coffeepeek.admin.utils.image.photoId
import com.coffeepeek.admin.utils.image.photoLog

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) = FullScreenImageDialog(imageUrls = listOf(imageUrl), initialIndex = 0, onDismiss = onDismiss)

/** Full-screen photo viewer; swipe horizontally between [imageUrls], tap to toggle controls, drag down to dismiss. */
@Composable
fun FullScreenImageDialog(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) = FullScreenImageDialog(PhotoViewerImages(imageUrls), initialIndex, onDismiss)

@Composable
internal fun FullScreenImageDialog(
    images: PhotoViewerImages,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val imageUrls = images.previews
    if (imageUrls.isEmpty()) return
    DisposableEffect(Unit) {
        FullScreenPhotoPresence.enter()
        onDispose { FullScreenPhotoPresence.leave() }
    }
    com.coffeepeek.admin.analytics.AnalyticsOcclusion()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size },
    )
    PrefetchPhotos(images.adjacentPreviewUrls(pagerState.currentPage), inPhotoViewer = true)
    PhotoViewerDialog(onDismiss = onDismiss) {
        PhotoViewerContent(images, pagerState, onDismiss)
    }
}

@Composable
internal expect fun PhotoViewerDialog(onDismiss: () -> Unit, content: @Composable () -> Unit)

@Composable
private fun PhotoViewerContent(images: PhotoViewerImages, pagerState: PagerState, onDismiss: () -> Unit) {
    // While the current photo is zoomed, drags pan the photo instead of switching pages.
    var isZoomed by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableFloatStateOf(1f) }
    val reduceMotion = platformReduceMotion()
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    val flingThreshold = with(LocalDensity.current) { 900.dp.toPx() }
    val dismissDrag = rememberDraggableState { dismissOffset = (dismissOffset + it).coerceAtLeast(0f) }
    PhotoViewerSurface(
        alpha = 0.8f * (1f - dismissOffset / viewportHeight).coerceIn(0f, 1f),
        modifier = Modifier
            .onSizeChanged { viewportHeight = it.height.toFloat().coerceAtLeast(1f) }
            .draggable(
                dismissDrag, Orientation.Vertical, enabled = !isZoomed,
                onDragStopped = { velocity ->
                    val dismiss = dismissOffset >= threshold || velocity >= flingThreshold
                    animate(
                        dismissOffset, if (dismiss) viewportHeight else 0f,
                        animationSpec = tween(if (reduceMotion) 0 else 180),
                    ) { value, _ -> dismissOffset = value }
                    if (dismiss) onDismiss()
                },
            ),
    ) {
        PhotoViewerGallery(
            images, pagerState, isZoomed, dismissOffset,
            onTap = { controlsVisible = !controlsVisible }, onZoomChanged = { isZoomed = it },
        )
        if (controlsVisible) PhotoViewerControls(pagerState.currentPage, images.previews.size, onDismiss)
    }
}

@Composable
private fun PhotoViewerSurface(
    alpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clipToBounds()
            .then(modifier),
        content = content,
    )
}

@Composable
private fun PhotoViewerGallery(
    images: PhotoViewerImages,
    pagerState: PagerState,
    isZoomed: Boolean,
    dismissOffset: Float,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    val visiblePages by remember(pagerState) {
        derivedStateOf { pagerState.layoutInfo.visiblePagesInfo.map { it.index }.toSet() }
    }
    HorizontalPager(
        state = pagerState,
        // Neighbor previews are prefetched separately without retaining offscreen painters.
        beyondViewportPageCount = 0,
        userScrollEnabled = !isZoomed,
        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = dismissOffset },
    ) { page ->
        val isCurrent = pagerState.currentPage == page
        val displayUrl = images.displayUrl(page, pagerState.currentPage, page in visiblePages)
        val scrolling = pagerState.isScrollInProgress
        LaunchedEffect(page, isCurrent, displayUrl, pagerState.settledPage, scrolling, page in visiblePages) {
            photoLog("viewer page=$page current=$isCurrent settled=${pagerState.settledPage} scrolling=$scrolling visible=${page in visiblePages} " +
                "display=${photoId(displayUrl)} highQuality=${photoId(images.highQualityUrl(page, pagerState.settledPage).takeUnless { scrolling })}")
        }
        if (isCurrent || page in visiblePages) ZoomableImage(
            imageUrl = displayUrl.orEmpty(),
            highQualityUrl = images.highQualityUrl(page, pagerState.settledPage)
                .takeUnless { pagerState.isScrollInProgress },
            contentDescription = "Фото ${page + 1} из ${images.previews.size}",
            isCurrent = isCurrent,
            onTap = onTap,
            onZoomChanged = { if (isCurrent) onZoomChanged(it) },
        )
    }
}

@Composable
private fun BoxScope.PhotoViewerControls(
    page: Int, count: Int, onDismiss: () -> Unit,
) {
    if (count > 1) {
        Text(
            text = "${page + 1} / $count",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = CpDimens.spacing4)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
                .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing1),
        )
    }

    PlatformGlassIconButton(
        icon = GlassControlIcon.Close,
        onClick = onDismiss,
        contentDescription = "Закрыть",
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(CpDimens.spacing3),
    ) {
        Icon(
            imageVector = CpIcons.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun PhotoViewerLightPreview() = PhotoViewerPreview(darkTheme = false)

@Preview
@Composable
private fun PhotoViewerDarkPreview() = PhotoViewerPreview(darkTheme = true)

@Composable
private fun PhotoViewerPreview(darkTheme: Boolean) {
    CoffeePeekTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            PhotoViewerSurface(alpha = 0.8f) {
                Image(
                    painter = ColorPainter(MaterialTheme.colorScheme.primary),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(240.dp),
                )
                PhotoViewerControls(page = 0, count = 3, onDismiss = {})
            }
        }
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Pinch to zoom (1–5×), drag to pan while zoomed, double-tap to zoom in/out at the tap point. */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    highQualityUrl: String?,
    contentDescription: String,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun clamp(value: Offset, forScale: Float): Offset {
        val maxX = size.width * (forScale - 1f) / 2f
        val maxY = size.height * (forScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // Swiped away → reset, so the photo is unzoomed when the user comes back to it.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    LaunchedEffect(scale > 1f) { onZoomChanged(scale > 1f) }

    CpImage(
        data = imageUrl,
        highQualityData = highQualityUrl?.takeIf { it != imageUrl },
        upgradeDelayMillis = 0L,
        showLoader = true,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        val startScale = scale
                        val startOffset = offset
                        val targetScale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val targetOffset = clamp((center - tap) * (targetScale - 1f), targetScale)
                        scope.launch {
                            animate(0f, 1f, animationSpec = tween(250)) { t, _ ->
                                scale = startScale + (targetScale - startScale) * t
                                offset = startOffset + (targetOffset - startOffset) * t
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.count { it.pressed } > 1
                        // One finger at 1× is left to the pager (page swipe) and tap detector.
                        if (pinching || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            // Zoom around the fingers' centroid, then apply the pan.
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val anchor = if (centroid.isSpecified) centroid - center else Offset.Zero
                            val scaled = (offset - anchor) * (newScale / scale) + anchor
                            offset = clamp(scaled + event.calculatePan(), newScale)
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale <= 1f) offset = Offset.Zero
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}
